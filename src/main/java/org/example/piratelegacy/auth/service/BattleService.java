package org.example.piratelegacy.auth.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.piratelegacy.auth.dto.BattleLogEntryDto;
import org.example.piratelegacy.auth.dto.BattlePirateDto;
import org.example.piratelegacy.auth.dto.BattleResultDto;
import org.example.piratelegacy.auth.dto.ItemRewardDto;
import org.example.piratelegacy.auth.entity.Quest;
import org.example.piratelegacy.auth.entity.enums.TeamType;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BattleService {

    private final QuestService questService;
    private static final Random random = new Random();

    private static final int MAX_PATHFINDING_ITERATIONS = 50;
    private static final int MAX_TURNS = 10000; // Защита от бесконечного цикла

    public BattleResultDto fight(String questKey, List<BattlePirateDto> initialPlacement) {
        Quest quest = questService.getQuestByKey(questKey);

        List<BattleUnit> allies = initialPlacement.stream()
                .filter(p -> p.getTeam() == TeamType.ALLY)
                .map(BattleUnit::new)
                .collect(Collectors.toCollection(ArrayList::new));

        List<BattleUnit> enemies = initialPlacement.stream()
                .filter(p -> p.getTeam() == TeamType.ENEMY)
                .map(BattleUnit::new)
                .collect(Collectors.toCollection(ArrayList::new));

        List<BattleLogEntryDto> battleLog = new ArrayList<>();
        int turnCounter = 0;

        // Основной цикл боя - непрерывный и динамичный
        while (!allies.isEmpty() && !enemies.isEmpty() && turnCounter < MAX_TURNS) {
            turnCounter++;

            List<BattleUnit> allLivingUnits = Stream.concat(allies.stream(), enemies.stream())
                    .collect(Collectors.toCollection(ArrayList::new));

            // Находим юнита, который действует следующим
            BattleUnit unitToAct = allLivingUnits.stream()
                    .min(Comparator.comparingDouble(BattleUnit::getActionTimer))
                    .orElse(null);

            if (unitToAct == null) break;

            // Проматываем время для всех юнитов
            double timeStep = unitToAct.getActionTimer();

            for (BattleUnit unit : allLivingUnits) {
                unit.advanceTime(timeStep);
            }

            // Юнит действует
            List<BattleUnit> targets = unitToAct.getTeam() == TeamType.ALLY ? enemies : allies;
            if (targets.isEmpty()) break;

            processUnitAction(unitToAct, targets, allLivingUnits, battleLog);
        }

        return buildBattleResult(quest, initialPlacement, allies, enemies, battleLog);
    }

    private void processUnitAction(BattleUnit actor, List<BattleUnit> targets,
                                   List<BattleUnit> allUnits, List<BattleLogEntryDto> log) {
        if (targets.isEmpty()) return;

        // Находим оптимальную цель
        BattleUnit target = findOptimalTarget(actor, targets);
        if (target == null) {
            actor.resetActionTimer(true);
            return;
        }

        int distance = calculateDistance(actor, target);

        if (distance <= actor.getRange()) {
            // АТАКА
            performAttack(actor, target, targets, allUnits, log);
            actor.resetActionTimer(false); // Перезарядка атаки
        } else {
            // ДВИЖЕНИЕ
            boolean moved = performIntelligentMovement(actor, target, allUnits, log);

            if (moved) {
                actor.resetActionTimer(true); // Короткая перезарядка после движения
            } else {
                actor.resetActionTimer(false); // Если застрял - стандартная перезарядка
            }
        }
    }

    /**
     * Простая и надежная атака без промахов и критов
     *
     * Механика урона:
     * 1. Базовый урон пирата (например 10-20) + бонус экипировки (например +5) = итоговый урон (15-25)
     * 2. Случайное значение из диапазона (например выпало 18)
     * 3. Вычитаем броню цели (например 18 - 3 = 15 урона)
     *
     * Примечание: Броня всегда меньше минимального урона, поэтому урон никогда не будет <= 0
     */
    private void performAttack(BattleUnit attacker, BattleUnit target,
                               List<BattleUnit> targets, List<BattleUnit> allUnits,
                               List<BattleLogEntryDto> log) {

        // Случайный урон в диапазоне min-max (уже включает бонусы от экипировки)
        int baseDamage = random.nextInt(attacker.getMaxAttack() - attacker.getMinAttack() + 1)
                + attacker.getMinAttack();

        // Броня вычитается из урона напрямую
        int finalDamage = baseDamage - target.getArmor();

        target.takeDamage(finalDamage);
        boolean isKill = target.isDead();

        Map<String, Object> attackData = new HashMap<>();
        attackData.put("targetId", target.getId());
        attackData.put("damage", finalDamage);
        attackData.put("isKill", isKill);
        attackData.put("remainingHp", Math.max(0, target.getCurrentHp()));
        attackData.put("targetMaxHp", target.getHp());

        log.add(new BattleLogEntryDto(
                BattleLogEntryDto.LogEntryType.ATTACK,
                attacker.getId(),
                attackData
        ));

        if (isKill) {
            targets.remove(target);
            allUnits.remove(target);

            log.add(new BattleLogEntryDto(
                    BattleLogEntryDto.LogEntryType.DEATH,
                    target.getId(),
                    Map.of("killerId", attacker.getId())
            ));
        }
    }

    /**
     * Интеллектуальное движение с A* и динамическим обходом препятствий
     * Пираты не застревают и всегда находят путь к врагу
     */
    private boolean performIntelligentMovement(BattleUnit mover, BattleUnit target,
                                               List<BattleUnit> allUnits,
                                               List<BattleLogEntryDto> log) {
        int startQ = mover.getQ();
        int startR = mover.getR();

        // Создаем карту занятых клеток (исключая текущего юнита)
        Set<String> obstacles = allUnits.stream()
                .filter(u -> u != mover)
                .map(u -> u.getQ() + ":" + u.getR())
                .collect(Collectors.toSet());

        // A* поиск пути
        List<HexCoord> path = findPathAStar(
                new HexCoord(startQ, startR),
                new HexCoord(target.getQ(), target.getR()),
                obstacles
        );

        HexCoord nextStep = null;

        if (path != null && path.size() > 1) {
            // Идем по найденному пути
            nextStep = path.get(1);
        } else {
            // Если A* не нашел путь, используем жадный алгоритм
            nextStep = findGreedyMove(mover, target, obstacles);
        }

        if (nextStep != null && (nextStep.q != startQ || nextStep.r != startR)) {
            mover.move(nextStep.q, nextStep.r);

            Map<String, Object> moveData = new HashMap<>();
            moveData.put("fromQ", startQ);
            moveData.put("fromR", startR);
            moveData.put("toQ", nextStep.q);
            moveData.put("toR", nextStep.r);
            moveData.put("targetId", target.getId());

            log.add(new BattleLogEntryDto(
                    BattleLogEntryDto.LogEntryType.MOVE,
                    mover.getId(),
                    moveData
            ));

            return true;
        }

        return false; // Не смог переместиться
    }

    /**
     * A* алгоритм для поиска оптимального пути
     */
    private List<HexCoord> findPathAStar(HexCoord start, HexCoord goal, Set<String> obstacles) {
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(
                Comparator.comparingDouble(AStarNode::getF)
        );

        Set<String> closedSet = new HashSet<>();
        Map<String, AStarNode> allNodes = new HashMap<>();

        AStarNode startNode = new AStarNode(start, null, 0,
                heuristicDistance(start, goal));
        openSet.offer(startNode);
        allNodes.put(start.toKey(), startNode);

        int[][] directions = {{1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}, {0, 1}};
        int iterations = 0;

        while (!openSet.isEmpty() && iterations < MAX_PATHFINDING_ITERATIONS) {
            iterations++;
            AStarNode current = openSet.poll();

            if (current == null) break;

            String currentKey = current.coord.toKey();
            if (closedSet.contains(currentKey)) continue;
            closedSet.add(currentKey);

            // Проверяем, достигли ли цели или соседней клетки (для атаки)
            int distToGoal = calculateDistance(current.coord.q, current.coord.r,
                    goal.q, goal.r);
            if (distToGoal <= 1) {
                return reconstructPath(current);
            }

            // Исследуем соседей
            for (int[] dir : directions) {
                HexCoord neighbor = new HexCoord(
                        current.coord.q + dir[0],
                        current.coord.r + dir[1]
                );

                String neighborKey = neighbor.toKey();

                // Пропускаем препятствия и посещенные клетки
                if (obstacles.contains(neighborKey) || closedSet.contains(neighborKey)) {
                    continue;
                }

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

        return null; // Путь не найден
    }

    /**
     * Жадный алгоритм - делаем шаг к цели, даже если путь частично заблокирован
     */
    private HexCoord findGreedyMove(BattleUnit mover, BattleUnit target, Set<String> obstacles) {
        int[][] directions = {{1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}, {0, 1}};

        HexCoord currentPos = new HexCoord(mover.getQ(), mover.getR());
        HexCoord targetPos = new HexCoord(target.getQ(), target.getR());

        int bestDistance = calculateDistance(currentPos.q, currentPos.r,
                targetPos.q, targetPos.r);
        HexCoord bestMove = null;

        // Проверяем все соседние клетки
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

        // Если не нашли клетку ближе к цели, попробуем любую свободную
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

    /**
     * Находим оптимальную цель:
     * 1. Приоритет - раненые враги (можно добить)
     * 2. Иначе - ближайший враг
     */
    private BattleUnit findOptimalTarget(BattleUnit attacker, List<BattleUnit> targets) {
        // Сначала ищем раненых врагов, которых можно добить за 1-2 удара
        int attackPower = (attacker.getMinAttack() + attacker.getMaxAttack()) / 2;

        BattleUnit wounded = targets.stream()
                .filter(t -> t.getCurrentHp() <= attackPower * 2) // Можно добить за 2 удара
                .min(Comparator.comparingInt(BattleUnit::getCurrentHp)
                        .thenComparingInt(t -> calculateDistance(attacker, t)))
                .orElse(null);

        if (wounded != null) return wounded;

        // Иначе просто ближайший враг
        return targets.stream()
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
                                    r.getItem().getName(), r.getItem().getImageUrl(),
                                    r.getQuantity()))
                            .collect(Collectors.toList()))
                    .build();
        } else {
            rewards = BattleResultDto.RewardsDto.builder()
                    .experience(0).gold(0).wood(0L).stone(0L).items(List.of())
                    .build();
        }

        return BattleResultDto.builder()
                .winnerTeam(winnerTeam)
                .rewards(rewards)
                .log(battleLog)
                .yourLossesIds(allyLossesIds)
                .enemyLossesIds(enemyLossesIds)
                .build();
    }

    // ===== ВСПОМОГАТЕЛЬНЫЕ КЛАССЫ =====

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
        final double g; // Стоимость пути от старта
        final double h; // Эвристика до цели
        final double f; // g + h

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
        private double actionTimer;
        private int range = 1; // Дальность атаки (можно расширить для лучников)

        public BattleUnit(BattlePirateDto dto) {
            super(dto.getId(), dto.getTeam(), dto.getHp(), dto.getMinAttack(),
                    dto.getMaxAttack(), dto.getArmor(), dto.getXp(), dto.getQ(),
                    dto.getR(), dto.getImageId(), dto.getMovement(), dto.getAttackSpeed());
            this.currentHp = dto.getHp();

            // Начальный таймер со случайным разбросом для разнообразия
            double baseSpeed = 100.0 / Math.max(10, getAttackSpeed());
            this.actionTimer = baseSpeed * (0.8 + random.nextDouble() * 0.4);
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

        public void advanceTime(double time) {
            this.actionTimer -= time;
        }

        public void resetActionTimer(boolean isMovement) {
            if (isMovement) {
                // Движение быстрее, чем атака (как во Флинте)
                double moveSpeed = 100.0 / Math.max(20, getMovement());
                this.actionTimer = moveSpeed * 0.3; // Движение в 3 раза быстрее
            } else {
                // Атака
                double attackSpeed = 100.0 / Math.max(10, getAttackSpeed());
                this.actionTimer = attackSpeed;
            }
        }
    }
}