package org.example.piratelegacy.auth.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.*;
import org.example.piratelegacy.auth.entity.Quest;
import org.example.piratelegacy.auth.entity.enums.TeamType;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BattleService {

    private final QuestService questService;
    private final BattleConfigService battleConfigService;
    private static final Random random = new Random();

    // === ПРОСТАЯ ОДНОВРЕМЕННАЯ СИСТЕМА ===
    // Все действия занимают одинаковое время
    private static final double ACTION_DURATION = 1.0;  // Каждое действие = 1 единица времени
    private static final int MAX_PATHFINDING_ITERATIONS = 50;
    private static final int MAX_TURNS = 1000;  // Уменьшили для безопасности
    private static final long TICK_DELAY_MS = 100;  // Задержка 100мс между тиками (10 действий/сек)

    public BattleResultDto fight(String questKey, List<BattlePirateDto> initialPlacement) {
        Quest quest = questService.getQuestByKey(questKey);

        LocationConfig locationConfig = battleConfigService.getLocationConfig(quest.getBattleLocationId());
        final Set<String> blockedCellsSet = locationConfig.getBlockedCells().stream()
                .map(cell -> cell.getQ() + ":" + cell.getR())
                .collect(Collectors.toSet());

        List<BattleUnit> allies = new ArrayList<>();
        List<BattleUnit> enemies = new ArrayList<>();
        List<BattleUnit> allLivingUnits = new ArrayList<>();

        initialPlacement.forEach(p -> {
            BattleUnit unit = new BattleUnit(p);
            allLivingUnits.add(unit);
            if (p.getTeam() == TeamType.ALLY) {
                allies.add(unit);
            } else {
                enemies.add(unit);
            }
        });

        List<BattleLogEntryDto> battleLog = new ArrayList<>();
        double globalTime = 0.0;
        int turnCounter = 0;

        // === КЛЮЧЕВОЕ ИЗМЕНЕНИЕ: ВСЕ ДЕЙСТВУЮТ ОДНОВРЕМЕННО ===
        while (!allies.isEmpty() && !enemies.isEmpty() && turnCounter < MAX_TURNS) {
            turnCounter++;
            globalTime += ACTION_DURATION;

            // Все юниты готовы к действию одновременно
            List<BattleUnit> unitsToAct = new ArrayList<>(allLivingUnits);

            // Перемешиваем для случайного порядка разрешения конфликтов
            Collections.shuffle(unitsToAct, random);

            // Каждый юнит планирует своё действие
            for (BattleUnit actor : unitsToAct) {
                if (actor.isDead()) continue;

                List<BattleUnit> targets = actor.getTeam() == TeamType.ALLY ? enemies : allies;
                if (targets.isEmpty()) break;

                processUnitAction(actor, targets, allLivingUnits, battleLog, globalTime, blockedCellsSet);
            }

            // Удаляем мертвых после того, как все действия выполнены
            allies.removeIf(BattleUnit::isDead);
            enemies.removeIf(BattleUnit::isDead);
            allLivingUnits.removeIf(BattleUnit::isDead);

            // Добавляем задержку между тиками для плавности
            try {
                Thread.sleep(TICK_DELAY_MS);
            } catch (InterruptedException e) {
                log.warn("Battle simulation interrupted", e);
                Thread.currentThread().interrupt();
                break;
            }
        }

        return buildBattleResult(quest, initialPlacement, allies, enemies, battleLog);
    }

    private void processUnitAction(BattleUnit actor, List<BattleUnit> targets,
                                   List<BattleUnit> allUnits, List<BattleLogEntryDto> log,
                                   double currentTime, Set<String> blockedCells) {
        if (targets.isEmpty() || actor.isDead()) return;

        BattleUnit target = findOptimalTarget(actor, targets);
        if (target == null) return;

        int distance = calculateDistance(actor, target);

        // Если в радиусе атаки - бьем
        if (distance <= actor.getRange()) {
            performAttack(actor, target, targets, allUnits, log, currentTime);
        } else {
            // Иначе двигаемся
            performIntelligentMovement(actor, target, allUnits, log, currentTime, blockedCells);
        }
    }

    private void performAttack(BattleUnit attacker, BattleUnit target,
                               List<BattleUnit> targets, List<BattleUnit> allUnits,
                               List<BattleLogEntryDto> log, double currentTime) {
        if (target.isDead()) return; // Цель уже мертва в этом тике

        int baseDamage = random.nextInt(attacker.getMaxAttack() - attacker.getMinAttack() + 1)
                + attacker.getMinAttack();
        int rawDamage = baseDamage - target.getArmor();
        int actualDamage = Math.max(0, rawDamage);
        target.takeDamage(actualDamage);
        boolean isKill = target.isDead();

        Map<String, Object> attackData = new HashMap<>();
        attackData.put("targetId", target.getId());
        attackData.put("damage", actualDamage);
        attackData.put("isKill", isKill);
        attackData.put("remainingHp", Math.max(0, target.getCurrentHp()));
        attackData.put("targetMaxHp", target.getHp());
        attackData.put("time", currentTime);
        attackData.put("duration", ACTION_DURATION);

        log.add(new BattleLogEntryDto(
                BattleLogEntryDto.LogEntryType.ATTACK,
                attacker.getId(),
                attackData
        ));

        if (isKill) {
            Map<String, Object> deathData = new HashMap<>();
            deathData.put("killerId", attacker.getId());
            deathData.put("time", currentTime);

            log.add(new BattleLogEntryDto(
                    BattleLogEntryDto.LogEntryType.DEATH,
                    target.getId(),
                    deathData
            ));
        }
    }

    private boolean performIntelligentMovement(BattleUnit mover, BattleUnit target,
                                               List<BattleUnit> allUnits,
                                               List<BattleLogEntryDto> log,
                                               double currentTime,
                                               Set<String> blockedCells) {
        int startQ = mover.getQ();
        int startR = mover.getR();

        // Собираем препятствия (заблокированные клетки + другие юниты)
        Set<String> obstacles = new HashSet<>(blockedCells);
        allUnits.stream()
                .filter(u -> u != mover && !u.isDead())
                .forEach(u -> obstacles.add(u.getQ() + ":" + u.getR()));

        // Ищем путь к цели
        List<HexCoord> path = findPathAStar(
                new HexCoord(startQ, startR),
                new HexCoord(target.getQ(), target.getR()),
                obstacles,
                mover.getRange()
        );

        if (path != null && path.size() > 1) {
            // Двигаемся ровно на ОДНУ клетку за ход
            HexCoord nextStep = path.get(1);

            // Проверяем, не занята ли следующая клетка другим юнитом
            String nextStepKey = nextStep.q + ":" + nextStep.r;
            boolean destinationBlocked = allUnits.stream()
                    .anyMatch(u -> u != mover && !u.isDead() &&
                            (u.getQ() + ":" + u.getR()).equals(nextStepKey));

            if (destinationBlocked) {
                // Клетка занята - не можем двигаться
                return false;
            }

            mover.move(nextStep.q, nextStep.r);

            Map<String, Object> moveData = new HashMap<>();
            moveData.put("fromQ", startQ);
            moveData.put("fromR", startR);
            moveData.put("toQ", nextStep.q);
            moveData.put("toR", nextStep.r);
            moveData.put("targetId", target.getId());
            moveData.put("time", currentTime);
            moveData.put("duration", ACTION_DURATION);

            log.add(new BattleLogEntryDto(
                    BattleLogEntryDto.LogEntryType.MOVE,
                    mover.getId(),
                    moveData
            ));
            return true;
        }

        // Fallback: жадное движение
        HexCoord greedyStep = findGreedyMove(mover, target, obstacles);
        if (greedyStep != null && (greedyStep.q != startQ || greedyStep.r != startR)) {
            mover.move(greedyStep.q, greedyStep.r);

            Map<String, Object> moveData = new HashMap<>();
            moveData.put("fromQ", startQ);
            moveData.put("fromR", startR);
            moveData.put("toQ", greedyStep.q);
            moveData.put("toR", greedyStep.r);
            moveData.put("targetId", target.getId());
            moveData.put("time", currentTime);
            moveData.put("duration", ACTION_DURATION);

            log.add(new BattleLogEntryDto(
                    BattleLogEntryDto.LogEntryType.MOVE,
                    mover.getId(),
                    moveData
            ));
            return true;
        }

        return false;
    }

    // === A* PATHFINDING ===
    private List<HexCoord> findPathAStar(HexCoord start, HexCoord goal, Set<String> obstacles, int attackRange) {
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(AStarNode::getF));
        Set<String> closedSet = new HashSet<>();
        Map<String, AStarNode> allNodes = new HashMap<>();

        AStarNode startNode = new AStarNode(start, null, 0, heuristicDistance(start, goal));
        openSet.offer(startNode);
        allNodes.put(start.toKey(), startNode);

        int[][] directions = {{1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}, {0, 1}};
        int iterations = 0;

        while (!openSet.isEmpty() && iterations < MAX_PATHFINDING_ITERATIONS) {
            iterations++;
            AStarNode current = openSet.poll();
            if (current == null) break;

            // Проверяем, достигли ли мы дистанции атаки
            int distToGoal = calculateDistance(current.coord.q, current.coord.r, goal.q, goal.r);
            if (distToGoal <= attackRange) {
                return reconstructPath(current);
            }

            String currentKey = current.coord.toKey();
            if (closedSet.contains(currentKey)) continue;
            closedSet.add(currentKey);

            for (int[] dir : directions) {
                HexCoord neighbor = new HexCoord(current.coord.q + dir[0], current.coord.r + dir[1]);
                String neighborKey = neighbor.toKey();

                if (obstacles.contains(neighborKey) || closedSet.contains(neighborKey)) continue;

                double tentativeG = current.g + 1;
                AStarNode neighborNode = allNodes.get(neighborKey);

                if (neighborNode == null || tentativeG < neighborNode.g) {
                    double h = heuristicDistance(neighbor, goal);
                    neighborNode = new AStarNode(neighbor, current, tentativeG, h);
                    allNodes.put(neighborKey, neighborNode);
                    openSet.offer(neighborNode);
                }
            }
        }
        return null;
    }

    private HexCoord findGreedyMove(BattleUnit mover, BattleUnit target, Set<String> obstacles) {
        int[][] directions = {{1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}, {0, 1}};
        HexCoord currentPos = new HexCoord(mover.getQ(), mover.getR());
        HexCoord targetPos = new HexCoord(target.getQ(), target.getR());
        int bestDistance = calculateDistance(currentPos.q, currentPos.r, targetPos.q, targetPos.r);
        HexCoord bestMove = null;

        for (int[] dir : directions) {
            int newQ = currentPos.q + dir[0];
            int newR = currentPos.r + dir[1];
            String key = newQ + ":" + newR;

            if (!obstacles.contains(key)) {
                int dist = calculateDistance(newQ, newR, targetPos.q, targetPos.r);
                if (dist < bestDistance) {
                    bestDistance = dist;
                    bestMove = new HexCoord(newQ, newR);
                }
            }
        }

        // Если не нашли лучший ход, попробуем случайный свободный
        if (bestMove == null) {
            List<int[]> shuffled = Arrays.asList(directions);
            Collections.shuffle(shuffled, random);
            for (int[] dir : shuffled) {
                int newQ = currentPos.q + dir[0];
                int newR = currentPos.r + dir[1];
                if (!obstacles.contains(newQ + ":" + newR)) {
                    return new HexCoord(newQ, newR);
                }
            }
        }
        return bestMove;
    }

    private BattleUnit findOptimalTarget(BattleUnit attacker, List<BattleUnit> targets) {
        int attackPower = (attacker.getMinAttack() + attacker.getMaxAttack()) / 2;

        // Приоритет: добить раненых
        BattleUnit wounded = targets.stream()
                .filter(t -> !t.isDead() && t.getCurrentHp() <= attackPower * 2)
                .min(Comparator.comparingInt(BattleUnit::getCurrentHp)
                        .thenComparingInt(t -> calculateDistance(attacker, t)))
                .orElse(null);

        if (wounded != null) return wounded;

        // Иначе выбираем ближайшего
        return targets.stream()
                .filter(t -> !t.isDead())
                .min(Comparator.comparingInt(t -> calculateDistance(attacker, t)))
                .orElse(null);
    }

    private double heuristicDistance(HexCoord a, HexCoord b) {
        return calculateDistance(a.q, a.r, b.q, b.r);
    }

    private List<HexCoord> reconstructPath(AStarNode endNode) {
        List<HexCoord> path = new ArrayList<>();
        AStarNode current = endNode;
        while (current != null) {
            path.add(0, current.coord);
            current = current.parent;
        }
        return path;
    }

    private int calculateDistance(int q1, int r1, int q2, int r2) {
        int s1 = -q1 - r1;
        int s2 = -q2 - r2;
        return (Math.abs(q1 - q2) + Math.abs(r1 - r2) + Math.abs(s1 - s2)) / 2;
    }

    private int calculateDistance(BattleUnit a, BattleUnit b) {
        return calculateDistance(a.getQ(), a.getR(), b.getQ(), b.getR());
    }

    private BattleResultDto buildBattleResult(Quest quest, List<BattlePirateDto> initialPlacement,
                                              List<BattleUnit> allies, List<BattleUnit> enemies,
                                              List<BattleLogEntryDto> battleLog) {
        TeamType winnerTeam = allies.isEmpty() ? TeamType.ENEMY : TeamType.ALLY;

        List<String> allyLossesIds = initialPlacement.stream()
                .filter(p -> p.getTeam() == TeamType.ALLY &&
                        allies.stream().noneMatch(u -> u.getId().equals(p.getId())))
                .map(BattlePirateDto::getId)
                .collect(Collectors.toList());

        List<String> enemyLossesIds = initialPlacement.stream()
                .filter(p -> p.getTeam() == TeamType.ENEMY &&
                        enemies.stream().noneMatch(u -> u.getId().equals(p.getId())))
                .map(BattlePirateDto::getId)
                .collect(Collectors.toList());

        BattleResultDto.RewardsDto rewards;
        if (winnerTeam == TeamType.ALLY) {
            rewards = BattleResultDto.RewardsDto.builder()
                    .experience(quest.getExpReward() != null ? quest.getExpReward() : 0L)
                    .gold(quest.getGoldReward() != null ? quest.getGoldReward() : 0L)
                    .wood(quest.getWoodReward() != null ? quest.getWoodReward() : 0L)
                    .stone(quest.getStoneReward() != null ? quest.getStoneReward() : 0L)
                    .items(quest.getItemRewards().stream()
                            .map(r -> new ItemRewardDto(r.getItem().getItemKey(),
                                    r.getItem().getName(), r.getItem().getImageUrl(), r.getQuantity()))
                            .collect(Collectors.toList()))
                    .build();
        } else {
            rewards = BattleResultDto.RewardsDto.builder()
                    .experience(0).gold(0).wood(0L).stone(0L).items(List.of()).build();
        }

        return BattleResultDto.builder()
                .winnerTeam(winnerTeam)
                .rewards(rewards)
                .log(battleLog)
                .yourLossesIds(allyLossesIds)
                .enemyLossesIds(enemyLossesIds)
                .build();
    }

    // === ВСПОМОГАТЕЛЬНЫЕ КЛАССЫ ===
    @Getter
    private static class HexCoord {
        final int q;
        final int r;

        HexCoord(int q, int r) {
            this.q = q;
            this.r = r;
        }

        String toKey() {
            return q + ":" + r;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof HexCoord)) return false;
            HexCoord hexCoord = (HexCoord) o;
            return q == hexCoord.q && r == hexCoord.r;
        }

        @Override
        public int hashCode() {
            return Objects.hash(q, r);
        }
    }

    @Getter
    private static class AStarNode {
        final HexCoord coord;
        final AStarNode parent;
        final double g;
        final double h;
        final double f;

        AStarNode(HexCoord coord, AStarNode parent, double g, double h) {
            this.coord = coord;
            this.parent = parent;
            this.g = g;
            this.h = h;
            this.f = g + h;
        }
    }

    @Getter
    @Setter
    private static class BattleUnit extends BattlePirateDto {
        private int currentHp;
        private int range;

        public BattleUnit(BattlePirateDto dto) {
            super(dto.getId(), dto.getTeam(), dto.getHp(), dto.getMinAttack(),
                    dto.getMaxAttack(), dto.getArmor(), dto.getXp(), dto.getQ(),
                    dto.getR(), dto.getImageId(), dto.getMovement(), dto.getAttackSpeed(),
                    dto.getRange());
            this.currentHp = dto.getHp();
            this.range = dto.getRange() > 0 ? dto.getRange() : 1;
        }

        public void takeDamage(int damage) {
            this.currentHp = Math.max(0, this.currentHp - damage);
        }

        public boolean isDead() {
            return this.currentHp <= 0;
        }

        public void move(int newQ, int newR) {
            this.setQ(newQ);
            this.setR(newR);
        }
    }
}