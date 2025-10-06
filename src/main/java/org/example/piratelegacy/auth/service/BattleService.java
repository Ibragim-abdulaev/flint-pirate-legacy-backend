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

    private static final double ACTION_DURATION = 1.5;
    private static final int MAX_PATHFINDING_ITERATIONS = 100;
    private static final double TIME_STEP = 0.1;

    private static abstract class Action {
        final BattleUnit actor;
        Action(BattleUnit actor) { this.actor = actor; }
        abstract void execute(List<BattleLogEntryDto> log, double currentTime, Map<String, BattleUnit> occupiedCells);
    }

    private static class AttackAction extends Action {
        final BattleUnit target;
        AttackAction(BattleUnit actor, BattleUnit target) { super(actor); this.target = target; }
        @Override
        void execute(List<BattleLogEntryDto> log, double currentTime, Map<String, BattleUnit> occupiedCells) {
            performAttack(actor, target, log, currentTime);
        }
    }

    private static class MoveAction extends Action {
        final HexCoord destination;
        MoveAction(BattleUnit actor, HexCoord destination) { super(actor); this.destination = destination; }
        @Override
        void execute(List<BattleLogEntryDto> log, double currentTime, Map<String, BattleUnit> occupiedCells) {
            executeMove(actor, destination, log, currentTime, occupiedCells);
        }
    }

    public BattleResultDto fight(String questKey, List<BattlePirateDto> initialPlacement) {
        Quest quest = questService.getQuestByKey(questKey);
        LocationConfig locationConfig = battleConfigService.getLocationConfig(quest.getBattleLocationId());
        final Set<String> blockedCellsSet = locationConfig.getBlockedCells().stream()
                .map(cell -> cell.getQ() + ":" + cell.getR())
                .collect(Collectors.toSet());

        List<BattleUnit> allies = new ArrayList<>();
        List<BattleUnit> enemies = new ArrayList<>();
        List<BattleUnit> allLivingUnits = new ArrayList<>();
        Map<String, BattleUnit> occupiedCells = new HashMap<>();

        initialPlacement.forEach(p -> {
            BattleUnit unit = new BattleUnit(p);
            unit.setNextActionTime(0.0);
            allLivingUnits.add(unit);
            occupiedCells.put(unit.getPositionKey(), unit);
            if (p.getTeam() == TeamType.ALLY) {
                allies.add(unit);
            } else {
                enemies.add(unit);
            }
        });

        List<BattleLogEntryDto> battleLog = new ArrayList<>();
        double currentTime = 0.0;
        PathfindingContext pathfindingContext = new PathfindingContext();

        while (!allies.isEmpty() && !enemies.isEmpty()) {
            currentTime += TIME_STEP;

            List<BattleUnit> unitsToAct = new ArrayList<>();
            for (BattleUnit unit : allLivingUnits) {
                if (!unit.isDead() && unit.getNextActionTime() <= currentTime) {
                    unitsToAct.add(unit);
                }
            }

            if (unitsToAct.isEmpty()) continue;

            shuffleInPlace(unitsToAct);

            List<Action> plannedActions = new ArrayList<>();
            Map<String, BattleUnit> reservedCells = new HashMap<>();

            for (BattleUnit actor : unitsToAct) {
                if (actor.isDead()) continue;

                List<BattleUnit> targets = actor.getTeam() == TeamType.ALLY ? enemies : allies;
                if (targets.isEmpty()) break;

                BattleUnit target = findOptimalTarget(actor, targets);
                if (target == null) continue;

                int distance = calculateDistance(actor, target);

                if (distance <= actor.getRange()) {
                    // В радиусе атаки - атакуем и сбрасываем путь
                    plannedActions.add(new AttackAction(actor, target));
                    actor.clearPath();
                } else {
                    // Вне радиуса - нужно двигаться
                    Set<String> pathfindingObstacles = new HashSet<>(blockedCellsSet);

                    for (BattleUnit u : allLivingUnits) {
                        if (u.isDead() || u == actor) continue;
                        pathfindingObstacles.add(u.getPositionKey());
                    }

                    pathfindingObstacles.addAll(reservedCells.keySet());

                    HexCoord nextStep = getNextMoveStep(actor, target, pathfindingObstacles, pathfindingContext);

                    if (nextStep != null) {
                        String nextStepKey = nextStep.toKey();
                        if (!occupiedCells.containsKey(nextStepKey) && !reservedCells.containsKey(nextStepKey)) {
                            plannedActions.add(new MoveAction(actor, nextStep));
                            reservedCells.put(nextStepKey, actor);
                        } else {
                            // Путь заблокирован - пересчитываем в следующий раз
                            actor.clearPath();
                        }
                    }
                }
            }

            for (Action action : plannedActions) {
                action.execute(battleLog, currentTime, occupiedCells);
                action.actor.setNextActionTime(currentTime + ACTION_DURATION);
            }

            removeDeadUnits(allies, allLivingUnits, occupiedCells);
            removeDeadUnits(enemies, allLivingUnits, occupiedCells);
        }

        return buildBattleResult(quest, initialPlacement, allies, enemies, battleLog);
    }

    /**
     * Получает следующий шаг движения, используя сохраненный путь или вычисляя новый
     */
    private HexCoord getNextMoveStep(BattleUnit mover, BattleUnit target, Set<String> obstacles, PathfindingContext context) {
        // Проверяем, есть ли сохраненный путь и актуален ли он
        if (mover.hasPath()) {
            HexCoord nextStepFromPath = mover.getNextStepFromPath();

            // Проверяем, что следующий шаг не заблокирован
            if (nextStepFromPath != null && !obstacles.contains(nextStepFromPath.toKey())) {
                // Проверяем, что путь всё ещё ведёт к цели (цель не сильно сместилась)
                HexCoord pathGoal = mover.getPathGoal();
                if (pathGoal != null) {
                    int goalDist = calculateDistance(pathGoal.q, pathGoal.r, target.getQ(), target.getR());
                    // Если цель сместилась больше чем на 2 клетки - пересчитываем путь
                    if (goalDist <= 2) {
                        mover.advancePath(); // Переходим к следующему шагу в пути
                        return nextStepFromPath;
                    }
                }
            }
            // Путь устарел - очищаем
            mover.clearPath();
        }

        // Вычисляем новый путь
        HexCoord currentPos = new HexCoord(mover.getQ(), mover.getR());

        // Ищем лучшую позицию для атаки
        List<HexCoord> attackPositions = findFreeAttackPositions(target, mover.getRange(), obstacles);
        HexCoord goalPosition;

        if (!attackPositions.isEmpty()) {
            goalPosition = findClosestPosition(currentPos, attackPositions);
        } else {
            // Если все позиции заняты, идём просто к цели
            goalPosition = new HexCoord(target.getQ(), target.getR());
        }

        if (goalPosition == null) {
            return findSimpleApproachMove(mover, target, obstacles);
        }

        // Строим путь через A*
        List<HexCoord> path = findPathAStar(currentPos, goalPosition, obstacles, context);

        if (path != null && path.size() > 1) {
            // Сохраняем путь в юните
            mover.setPath(path, goalPosition);
            mover.advancePath(); // Пропускаем первый элемент (текущая позиция)
            return path.get(1);
        }

        // A* не нашёл путь - используем простое приближение
        return findSimpleApproachMove(mover, target, obstacles);
    }

    /**
     * Простое приближение к цели без сложных вычислений
     */
    private HexCoord findSimpleApproachMove(BattleUnit mover, BattleUnit target, Set<String> obstacles) {
        int[][] directions = {
                {1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}, {0, 1}
        };

        HexCoord currentPos = new HexCoord(mover.getQ(), mover.getR());
        HexCoord targetPos = new HexCoord(target.getQ(), target.getR());
        int currentDistance = calculateDistance(currentPos.q, currentPos.r, targetPos.q, targetPos.r);

        List<HexCoord> approachingMoves = new ArrayList<>();

        for (int[] dir : directions) {
            int newQ = currentPos.q + dir[0];
            int newR = currentPos.r + dir[1];
            String key = newQ + ":" + newR;

            if (!obstacles.contains(key)) {
                int newDistance = calculateDistance(newQ, newR, targetPos.q, targetPos.r);
                if (newDistance < currentDistance) {
                    approachingMoves.add(new HexCoord(newQ, newR));
                }
            }
        }

        if (!approachingMoves.isEmpty()) {
            return approachingMoves.get(random.nextInt(approachingMoves.size()));
        }

        // Если не можем приблизиться, пробуем боковой ход
        for (int[] dir : directions) {
            int newQ = currentPos.q + dir[0];
            int newR = currentPos.r + dir[1];
            String key = newQ + ":" + newR;

            if (!obstacles.contains(key)) {
                return new HexCoord(newQ, newR);
            }
        }

        return null;
    }

    private List<HexCoord> findFreeAttackPositions(BattleUnit target, int range, Set<String> obstacles) {
        List<HexCoord> positions = new ArrayList<>();
        int targetQ = target.getQ();
        int targetR = target.getR();

        for (int dq = -range; dq <= range; dq++) {
            for (int dr = -range; dr <= range; dr++) {
                int q = targetQ + dq;
                int r = targetR + dr;
                int dist = calculateDistance(q, r, targetQ, targetR);

                if (dist > 0 && dist <= range) {
                    String key = q + ":" + r;
                    if (!obstacles.contains(key)) {
                        positions.add(new HexCoord(q, r));
                    }
                }
            }
        }

        return positions;
    }

    private HexCoord findClosestPosition(HexCoord from, List<HexCoord> positions) {
        if (positions.isEmpty()) return null;

        HexCoord closest = null;
        int minDist = Integer.MAX_VALUE;

        for (HexCoord pos : positions) {
            int dist = calculateDistance(from.q, from.r, pos.q, pos.r);
            if (dist < minDist) {
                minDist = dist;
                closest = pos;
            }
        }

        return closest;
    }

    private List<HexCoord> findPathAStar(HexCoord start, HexCoord goal, Set<String> obstacles, PathfindingContext context) {
        context.clear();

        AStarNode startNode = new AStarNode(start, null, 0, heuristicDistance(start, goal));
        context.openSet.offer(startNode);
        context.allNodes.put(start.toKey(), startNode);

        int[][] directions = {{1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}, {0, 1}};
        int iterations = 0;

        while (!context.openSet.isEmpty() && iterations < MAX_PATHFINDING_ITERATIONS) {
            iterations++;
            AStarNode current = context.openSet.poll();
            if (current == null) break;

            if (current.coord.equals(goal)) {
                return reconstructPath(current);
            }

            String currentKey = current.coord.toKey();
            if (context.closedSet.contains(currentKey)) continue;
            context.closedSet.add(currentKey);

            for (int[] dir : directions) {
                HexCoord neighbor = new HexCoord(current.coord.q + dir[0], current.coord.r + dir[1]);
                String neighborKey = neighbor.toKey();

                if (obstacles.contains(neighborKey) || context.closedSet.contains(neighborKey)) continue;

                double tentativeG = current.g + 1;
                AStarNode neighborNode = context.allNodes.get(neighborKey);

                if (neighborNode == null || tentativeG < neighborNode.g) {
                    double h = heuristicDistance(neighbor, goal);
                    if (neighborNode != null) {
                        context.openSet.remove(neighborNode);
                    }
                    neighborNode = new AStarNode(neighbor, current, tentativeG, h);
                    context.allNodes.put(neighborKey, neighborNode);
                    context.openSet.offer(neighborNode);
                }
            }
        }

        return null;
    }

    private static void executeMove(BattleUnit mover, HexCoord destination, List<BattleLogEntryDto> log, double currentTime, Map<String, BattleUnit> occupiedCells) {
        int startQ = mover.getQ();
        int startR = mover.getR();
        String oldPositionKey = mover.getPositionKey();

        occupiedCells.remove(oldPositionKey);
        mover.move(destination.q, destination.r);
        occupiedCells.put(mover.getPositionKey(), mover);

        Map<String, Object> moveData = new HashMap<>();
        moveData.put("fromQ", startQ);
        moveData.put("fromR", startR);
        moveData.put("toQ", destination.q);
        moveData.put("toR", destination.r);
        moveData.put("targetId", "not_needed");
        moveData.put("time", currentTime);
        moveData.put("duration", ACTION_DURATION);
        log.add(new BattleLogEntryDto(BattleLogEntryDto.LogEntryType.MOVE, mover.getId(), moveData));
    }

    private void removeDeadUnits(List<BattleUnit> teamList, List<BattleUnit> allUnitsList, Map<String, BattleUnit> occupiedCells) {
        List<BattleUnit> deadUnitsInTeam = new ArrayList<>();
        Iterator<BattleUnit> iterator = teamList.iterator();
        while(iterator.hasNext()){
            BattleUnit unit = iterator.next();
            if (unit.isDead()){
                deadUnitsInTeam.add(unit);
                occupiedCells.remove(unit.getPositionKey());
                iterator.remove();
            }
        }
        allUnitsList.removeAll(deadUnitsInTeam);
    }

    private void shuffleInPlace(List<BattleUnit> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            BattleUnit temp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, temp);
        }
    }

    private static void performAttack(BattleUnit attacker, BattleUnit target, List<BattleLogEntryDto> log, double currentTime) {
        if (target.isDead()) return;

        int baseDamage = random.nextInt(attacker.getMaxAttack() - attacker.getMinAttack() + 1) + attacker.getMinAttack();
        int actualDamage = Math.max(0, baseDamage - target.getArmor());
        target.takeDamage(actualDamage);
        boolean isKill = target.isDead();

        Map<String, Object> attackData = new HashMap<>();
        attackData.put("targetId", target.getId());
        attackData.put("damage", actualDamage);
        attackData.put("isKill", isKill);
        attackData.put("remainingHp", target.getCurrentHp());
        attackData.put("targetMaxHp", target.getHp());
        attackData.put("time", currentTime);
        attackData.put("duration", ACTION_DURATION);
        log.add(new BattleLogEntryDto(BattleLogEntryDto.LogEntryType.ATTACK, attacker.getId(), attackData));

        if (isKill) {
            Map<String, Object> deathData = new HashMap<>();
            deathData.put("killerId", attacker.getId());
            deathData.put("time", currentTime);
            log.add(new BattleLogEntryDto(BattleLogEntryDto.LogEntryType.DEATH, target.getId(), deathData));
        }
    }

    private BattleUnit findOptimalTarget(BattleUnit attacker, List<BattleUnit> targets) {
        int attackPower = (attacker.getMinAttack() + attacker.getMaxAttack()) / 2;
        int attackRange = attacker.getRange();
        BattleUnit bestInRange = null;
        boolean foundKillableInRange = false;
        int lowestHpInRange = Integer.MAX_VALUE;
        int closestInRange = Integer.MAX_VALUE;

        for (BattleUnit t : targets) {
            if (t.isDead()) continue;
            int dist = calculateDistance(attacker, t);

            if (dist <= attackRange) {
                boolean canKill = t.getCurrentHp() <= attackPower;
                int hp = t.getCurrentHp();

                if (canKill && !foundKillableInRange) {
                    bestInRange = t;
                    foundKillableInRange = true;
                    lowestHpInRange = hp;
                    closestInRange = dist;
                } else if (canKill && foundKillableInRange) {
                    if (hp < lowestHpInRange || (hp == lowestHpInRange && dist < closestInRange)) {
                        bestInRange = t;
                        lowestHpInRange = hp;
                        closestInRange = dist;
                    }
                } else if (!foundKillableInRange) {
                    if (hp < lowestHpInRange || (hp == lowestHpInRange && dist < closestInRange)) {
                        bestInRange = t;
                        lowestHpInRange = hp;
                        closestInRange = dist;
                    }
                }
            }
        }

        if (bestInRange != null) {
            return bestInRange;
        }

        BattleUnit closestTarget = null;
        int minDistance = Integer.MAX_VALUE;
        for (BattleUnit t : targets) {
            if (t.isDead()) continue;
            int dist = calculateDistance(attacker, t);
            if (dist < minDistance) {
                minDistance = dist;
                closestTarget = t;
            }
        }
        return closestTarget;
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

    private BattleResultDto buildBattleResult(Quest quest, List<BattlePirateDto> initialPlacement, List<BattleUnit> allies, List<BattleUnit> enemies, List<BattleLogEntryDto> battleLog) {
        TeamType winnerTeam = allies.isEmpty() ? TeamType.ENEMY : TeamType.ALLY;
        List<String> allyLossesIds = initialPlacement.stream().filter(p -> p.getTeam() == TeamType.ALLY && allies.stream().noneMatch(u -> u.getId().equals(p.getId()))).map(BattlePirateDto::getId).collect(Collectors.toList());
        List<String> enemyLossesIds = initialPlacement.stream().filter(p -> p.getTeam() == TeamType.ENEMY && enemies.stream().noneMatch(u -> u.getId().equals(p.getId()))).map(BattlePirateDto::getId).collect(Collectors.toList());
        BattleResultDto.RewardsDto rewards;
        if (winnerTeam == TeamType.ALLY) {
            rewards = BattleResultDto.RewardsDto.builder().experience(quest.getExpReward() != null ? quest.getExpReward() : 0L).gold(quest.getGoldReward() != null ? quest.getGoldReward() : 0L).wood(quest.getWoodReward() != null ? quest.getWoodReward() : 0L).stone(quest.getStoneReward() != null ? quest.getStoneReward() : 0L).items(quest.getItemRewards().stream().map(r -> new ItemRewardDto(r.getItem().getItemKey(), r.getItem().getName(), r.getItem().getImageUrl(), r.getQuantity())).collect(Collectors.toList())).build();
        } else {
            rewards = BattleResultDto.RewardsDto.builder().experience(0L).gold(0L).wood(0L).stone(0L).items(Collections.emptyList()).build();
        }
        return BattleResultDto.builder().winnerTeam(winnerTeam).rewards(rewards).log(battleLog).yourLossesIds(allyLossesIds).enemyLossesIds(enemyLossesIds).build();
    }

    private static class PathfindingContext {
        final PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(AStarNode::getF));
        final Set<String> closedSet = new HashSet<>();
        final Map<String, AStarNode> allNodes = new HashMap<>();
        void clear() {
            openSet.clear();
            closedSet.clear();
            allNodes.clear();
        }
    }

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
        private String positionKey;
        private double nextActionTime;

        // Сохраненный путь
        private List<HexCoord> savedPath;
        private int pathIndex;
        private HexCoord pathGoal;

        public BattleUnit(BattlePirateDto dto) {
            super(dto.getId(), dto.getTeam(), dto.getHp(), dto.getMinAttack(), dto.getMaxAttack(), dto.getArmor(), dto.getXp(), dto.getQ(), dto.getR(), dto.getImageId(), dto.getMovement(), dto.getAttackSpeed(), dto.getRange());
            this.currentHp = dto.getHp();
            this.range = dto.getRange() > 0 ? dto.getRange() : 1;
            this.positionKey = dto.getQ() + ":" + dto.getR();
            this.nextActionTime = 0.0;
            this.savedPath = null;
            this.pathIndex = 0;
            this.pathGoal = null;
        }

        public void setPath(List<HexCoord> path, HexCoord goal) {
            this.savedPath = new ArrayList<>(path);
            this.pathIndex = 0;
            this.pathGoal = goal;
        }

        public void clearPath() {
            this.savedPath = null;
            this.pathIndex = 0;
            this.pathGoal = null;
        }

        public boolean hasPath() {
            return savedPath != null && pathIndex < savedPath.size();
        }

        public HexCoord getNextStepFromPath() {
            if (hasPath() && pathIndex + 1 < savedPath.size()) {
                return savedPath.get(pathIndex + 1);
            }
            return null;
        }

        public void advancePath() {
            if (savedPath != null) {
                pathIndex++;
                if (pathIndex >= savedPath.size()) {
                    clearPath();
                }
            }
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
            this.positionKey = newQ + ":" + newR;
        }
    }
}