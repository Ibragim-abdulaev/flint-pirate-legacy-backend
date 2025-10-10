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
    private static final int MAX_EMPTY_ITERATIONS = 1000;
    private static final double MAX_BATTLE_TIME = 300.0;
    private static final double THINK_DURATION = 0.5;
    private static final int MAX_PATH_BLOCKED_ATTEMPTS = 3;

    private static final double TARGET_SWITCH_COOLDOWN = 2.0;

    public BattleResultDto fight(String questKey, List<BattlePirateDto> initialPlacement) {
        Quest quest = questService.getQuestByKey(questKey);
        LocationConfig locationConfig = battleConfigService.getLocationConfig(quest.getBattleLocationId());
        final Set<String> blockedCellsSet = (locationConfig == null || locationConfig.getBlockedCells() == null)
                ? Collections.emptySet()
                : locationConfig.getBlockedCells().stream()
                .map(cell -> cell.getQ() + ":" + cell.getR())
                .collect(Collectors.toSet());

        List<BattleUnit> allies = new ArrayList<>();
        List<BattleUnit> enemies = new ArrayList<>();
        List<BattleUnit> allLivingUnits = new ArrayList<>();
        Map<String, BattleUnit> occupiedCells = new HashMap<>();

        if (initialPlacement != null) {
            initialPlacement.forEach(p -> {
                BattleUnit unit = new BattleUnit(p);
                allLivingUnits.add(unit);
                occupiedCells.put(unit.getPositionKey(), unit);
                if (p.getTeam() == TeamType.ALLY) {
                    allies.add(unit);
                } else {
                    enemies.add(unit);
                }
            });
        }

        PathfindingContext pathfindingContext = new PathfindingContext();

        List<BattleLogEntryDto> battleLog = new ArrayList<>();

        double currentTime = 0.0;
        int emptyIterations = 0;

        while (!allies.isEmpty() && !enemies.isEmpty() && currentTime < MAX_BATTLE_TIME) {
            currentTime += TIME_STEP;
            double finalCurrentTime = currentTime;

            List<BattleUnit> unitsToAct = allLivingUnits.stream()
                    .filter(u -> !u.isDead() && u.getNextActionTime() <= finalCurrentTime)
                    .sorted(Comparator.comparing(BattleUnit::getId))
                    .collect(Collectors.toList());

            if (unitsToAct.isEmpty()) {
                emptyIterations++;
                if (emptyIterations > MAX_EMPTY_ITERATIONS) break;
                continue;
            }
            emptyIterations = 0;

            Map<BattleUnit, PlannedAction> plannedActions = new HashMap<>();
            Set<String> claimedHexes = new HashSet<>();
            Set<String> claimedNextSteps = new HashSet<>();

            for (BattleUnit actor : unitsToAct) {
                if (actor.isDead()) continue;

                List<BattleUnit> targets = actor.getTeam() == TeamType.ALLY ? enemies : allies;
                List<BattleUnit> allies_for_pathfinding = actor.getTeam() == TeamType.ALLY ? allies : enemies;
                if (targets.isEmpty()) continue;

                Set<String> hardObstacles = new HashSet<>(blockedCellsSet);
                Set<String> softObstacles = new HashSet<>(claimedNextSteps);
                for (BattleUnit u : allLivingUnits) {
                    if (u == actor || u.isDead()) continue;
                    (u.getTeam() == actor.getTeam() ? softObstacles : hardObstacles).add(u.getPositionKey());
                }

                PlannedAction action = planAction(actor, targets, hardObstacles, softObstacles, pathfindingContext, allies_for_pathfinding, currentTime, claimedHexes);

                if (action != null) {
                    plannedActions.put(actor, action);
                    if (!action.isAttack) {
                        if (actor.getPathGoal() != null) {
                            claimedHexes.add(actor.getPathGoal().toKey());
                        }
                        if (action.nextStep != null) {
                            claimedNextSteps.add(action.nextStep.toKey());
                        }
                    }
                }
            }

            Map<String, BattleUnit> nextOccupiedCells = new HashMap<>(occupiedCells);

            for (BattleUnit actor : unitsToAct) {
                if (actor.isDead() || !plannedActions.containsKey(actor)) continue;

                PlannedAction action = plannedActions.get(actor);
                actor.setCurrentTarget(action.target);

                if (action.isAttack) {
                    performAttack(actor, action.target, battleLog, currentTime);
                    actor.clearPath();
                    actor.setNextActionTime(currentTime + ACTION_DURATION);
                } else if (action.nextStep != null) {
                    String newKey = action.nextStep.toKey();
                    if (!nextOccupiedCells.containsKey(newKey) || nextOccupiedCells.get(newKey) == actor) {
                        nextOccupiedCells.remove(actor.getPositionKey());
                        executeMove(actor, action.nextStep, battleLog, currentTime, occupiedCells);
                        nextOccupiedCells.put(newKey, actor);
                        actor.setNextActionTime(currentTime + ACTION_DURATION);
                        actor.setPathBlockedAttempts(0);
                    } else {
                        actor.incrementPathBlockedAttempts();
                        actor.clearPath();
                        double thinkTime = actor.getPathBlockedAttempts() >= 2 ? 0.1 : THINK_DURATION;
                        actor.setNextActionTime(currentTime + thinkTime);
                    }
                } else {
                    actor.setNextActionTime(currentTime + THINK_DURATION);
                }
            }

            removeDeadUnits(allies, allLivingUnits, occupiedCells);
            removeDeadUnits(enemies, allLivingUnits, occupiedCells);
        }

        return buildBattleResult(quest, initialPlacement, allies, enemies, battleLog);
    }

    private static class PlannedAction {
        final BattleUnit target;
        final HexCoord nextStep;
        final boolean isAttack;
        PlannedAction(BattleUnit target, HexCoord nextStep, boolean isAttack) {
            this.target = target; this.nextStep = nextStep; this.isAttack = isAttack;
        }
    }

    private PlannedAction planAction(
            BattleUnit actor, List<BattleUnit> targets,
            Set<String> hardObstacles, Set<String> softObstacles,
            PathfindingContext context, List<BattleUnit> allies,
            double currentTime, Set<String> claimedHexes) {

        BattleUnit targetToActOn = actor.getCurrentTarget();

        // ПРИОРИТЕТ 1: Враг вплотную
        Optional<BattleUnit> adjacentThreatOpt = targets.stream()
                .filter(t -> !t.isDead() && calculateAttackDistance(actor, t) <= 1)
                .min(Comparator.comparingInt(BattleUnit::getCurrentHp));

        if (adjacentThreatOpt.isPresent()) {
            BattleUnit adjacentThreat = adjacentThreatOpt.get();
            if (targetToActOn == null || !targetToActOn.getId().equals(adjacentThreat.getId())) {
                targetToActOn = adjacentThreat;
                actor.setCurrentTarget(targetToActOn);
                actor.setLastTargetSwitchTime(currentTime);
                actor.clearPath();
            }
        }

        // ПРИОРИТЕТ 2: Бьем текущую цель, если в зоне
        if (targetToActOn != null && !targetToActOn.isDead()) {
            int distanceToCurrentTarget = calculateAttackDistance(actor, targetToActOn);

            // КЛЮЧЕВОЕ: если деремся вплотную - НЕ БРОСАЕМ цель!
            if (distanceToCurrentTarget == 1) {
                actor.clearPath();
                return new PlannedAction(targetToActOn, null, true);
            }

            if (distanceToCurrentTarget <= actor.getRange()) {
                actor.clearPath();
                return new PlannedAction(targetToActOn, null, true);
            }
        }

        // ПРИОРИТЕТ 3: Ищем новую цель если застряли
        if (actor.getPathBlockedAttempts() >= MAX_PATH_BLOCKED_ATTEMPTS) {
            BattleUnit ignoredTarget = targetToActOn;
            targetToActOn = findOptimalTarget(actor, targets, ignoredTarget);
            actor.setCurrentTarget(targetToActOn);
            actor.setPathBlockedAttempts(0);
            actor.clearPath();
        }

        if (targetToActOn == null || targetToActOn.isDead()) {
            targetToActOn = findOptimalTarget(actor, targets, null);
            actor.setCurrentTarget(targetToActOn);
            actor.clearPath();
        }

        if (targetToActOn == null) {
            return null;
        }

        // ПРИОРИТЕТ 4: Двигаемся к цели
        if (calculateAttackDistance(actor, targetToActOn) <= actor.getRange()) {
            actor.clearPath();
            return new PlannedAction(targetToActOn, null, true);
        }

        if (actor.hasPath() && actor.getCurrentTarget() != null && actor.getCurrentTarget().getId().equals(targetToActOn.getId())) {
            HexCoord nextStep = actor.getNextStepFromPath();
            if (nextStep != null && !hardObstacles.contains(nextStep.toKey())) {
                if (!softObstacles.contains(nextStep.toKey())) {
                    return new PlannedAction(targetToActOn, nextStep, false);
                } else {
                    actor.clearPath();
                }
            } else {
                actor.clearPath();
            }
        }

        HexCoord currentPos = new HexCoord(actor.getQ(), actor.getR());
        PlannedAction finalPlan = generatePlanForTarget(actor, targetToActOn, currentPos, hardObstacles, softObstacles, context, allies, targets, claimedHexes);

        // Если не получилось - строим БЕЗ учета союзников
        if (finalPlan == null) {
            finalPlan = generatePlanForTarget(actor, targetToActOn, currentPos, hardObstacles, Collections.emptySet(), context, allies, targets, claimedHexes);
        }

        if (finalPlan == null) {
            HexCoord greedyStep = findGreedyStepTowards(currentPos, new HexCoord(targetToActOn.getQ(), targetToActOn.getR()), hardObstacles, softObstacles);
            if (greedyStep != null) {
                return new PlannedAction(targetToActOn, greedyStep, false);
            }
        }

        return finalPlan;
    }

    private int countAlliesNear(HexCoord position, List<BattleUnit> allies, int radius) {
        int count = 0;
        if (position == null || allies == null) return 0;
        for (BattleUnit ally : allies) {
            if (calculateDistance(position.q, position.r, ally.getQ(), ally.getR()) <= radius) count++;
        }
        return count;
    }

    private PlannedAction generatePlanForTarget(
            BattleUnit actor, BattleUnit target, HexCoord currentPos,
            Set<String> hardObstacles, Set<String> softObstacles,
            PathfindingContext context, List<BattleUnit> actorsTeam,
            List<BattleUnit> enemyTeam, Set<String> claimedHexes) {

        if (calculateAttackDistance(actor, target) <= actor.getRange()) {
            return new PlannedAction(target, null, true);
        }

        List<HexCoord> attackPositions = findPotentialAttackPositions(target, actor.getRange(), hardObstacles, claimedHexes, actorsTeam);

        if (attackPositions.isEmpty()) return null;

        HexCoord bestPosition = null;
        List<HexCoord> bestPath = null;
        double bestScore = Double.MAX_VALUE;

        Collections.shuffle(attackPositions);

        for (HexCoord pos : attackPositions) {
            // Пропускаем, если эта позиция уже зарезервирована другим юнитом
            if (claimedHexes.contains(pos.toKey())) {
                continue;
            }

            List<HexCoord> path = findPathAStarWithSoftObstacles(currentPos, pos, hardObstacles, softObstacles, context);
            if (path != null && !path.isEmpty()) {
                int crowdPenalty = countAlliesNear(pos, actorsTeam, 2);
                // Небольшой штраф за скопление, но не критичный
                double score = path.size() + (crowdPenalty * 0.5);
                if (score < bestScore) {
                    bestScore = score;
                    bestPosition = pos;
                    bestPath = path;
                }
            }
        }

        if (bestPosition != null && bestPath != null && bestPath.size() > 1) {
            actor.setPath(bestPath, bestPosition);
            return new PlannedAction(target, bestPath.get(1), false);
        }
        return null;
    }

    private HexCoord findGreedyStepTowards(HexCoord start, HexCoord goal, Set<String> hardObstacles, Set<String> softObstacles) {
        if (start == null || goal == null) return null;

        int[][] directions = {{1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}, {0, 1}};

        HexCoord best = null;
        HexCoord softBest = null;
        int bestDist = calculateDistance(start.q, start.r, goal.q, goal.r);
        int softBestDist = bestDist;

        for (int[] d : directions) {
            HexCoord neighbor = new HexCoord(start.q + d[0], start.r + d[1]);
            if (hardObstacles != null && hardObstacles.contains(neighbor.toKey())) continue;

            int dist = calculateDistance(neighbor.q, neighbor.r, goal.q, goal.r);
            boolean isSoft = softObstacles != null && softObstacles.contains(neighbor.toKey());

            if (!isSoft && dist < bestDist) {
                bestDist = dist;
                best = neighbor;
            } else if (isSoft && dist < softBestDist) {
                softBestDist = dist;
                softBest = neighbor;
            }
        }

        return best != null ? best : softBest;
    }

    private List<HexCoord> findFreeAttackPositions(BattleUnit target, int range, Set<String> obstacles, Set<String> claimedHexes) {
        List<HexCoord> positions = new ArrayList<>();
        if (target == null) return positions;
        int targetQ = target.getQ();
        int targetR = target.getR();

        for (int q = targetQ - range; q <= targetQ + range; q++) {
            for (int r = targetR - range; r <= targetR + range; r++) {
                HexCoord current = new HexCoord(q, r);
                int dist = calculateDistance(q, r, targetQ, targetR);
                if (dist > 0 && dist <= range) {
                    String key = current.toKey();
                    if ((obstacles == null || !obstacles.contains(key)) && (claimedHexes == null || !claimedHexes.contains(key))) {
                        positions.add(current);
                    }
                }
            }
        }
        return positions;
    }

    private List<HexCoord> findPotentialAttackPositions(BattleUnit target, int range,
                                                        Set<String> hardObstacles,
                                                        Set<String> claimedHexes,
                                                        List<BattleUnit> allies) {
        List<HexCoord> freePositions = new ArrayList<>();
        List<HexCoord> allyOccupiedPositions = new ArrayList<>();
        if (target == null) return freePositions;

        Set<String> allyPositions = allies.stream()
                .map(BattleUnit::getPositionKey)
                .collect(Collectors.toSet());

        int targetQ = target.getQ();
        int targetR = target.getR();

        for (int q = targetQ - range; q <= targetQ + range; q++) {
            for (int r = targetR - range; r <= targetR + range; r++) {
                HexCoord current = new HexCoord(q, r);
                if (calculateDistance(q, r, targetQ, targetR) > range) continue;

                String key = current.toKey();
                if (hardObstacles.contains(key) || claimedHexes.contains(key)) continue;

                if (allyPositions.contains(key)) {
                    allyOccupiedPositions.add(current);
                } else {
                    freePositions.add(current);
                }
            }
        }

        return !freePositions.isEmpty() ? freePositions : allyOccupiedPositions;
    }

    private List<HexCoord> findPathAStarWithSoftObstacles(
            HexCoord start, HexCoord goal,
            Set<String> hardObstacles, Set<String> softObstacles,
            PathfindingContext context) {

        if (start == null || goal == null || context == null) return null;
        context.clear();

        if (start.equals(goal)) {
            return List.of(start);
        }

        AStarNode startNode = new AStarNode(start, null, 0, heuristicDistance(start, goal));
        context.openSet.offer(startNode);
        context.allNodes.put(start.toKey(), startNode);

        int[][] directions = {
                {1, 0}, {1, -1}, {0, -1},
                {-1, 0}, {-1, 1}, {0, 1}
        };

        int iterations = 0;
        while (!context.openSet.isEmpty() && iterations < MAX_PATHFINDING_ITERATIONS) {
            iterations++;
            AStarNode current = context.openSet.poll();
            if (current == null) break;

            if (current.coord.equals(goal)) {
                return reconstructPath(current);
            }

            context.closedSet.add(current.coord.toKey());

            for (int[] dir : directions) {
                HexCoord neighbor = new HexCoord(current.coord.q + dir[0], current.coord.r + dir[1]);
                String neighborKey = neighbor.toKey();

                if (hardObstacles != null && hardObstacles.contains(neighborKey) || context.closedSet.contains(neighborKey))
                    continue;

                double moveCost = 1.0;
                if (softObstacles != null && softObstacles.contains(neighborKey)) {
                    moveCost += 1.5;
                }

                double tentativeG = current.g + moveCost;
                AStarNode neighborNode = context.allNodes.get(neighborKey);

                if (neighborNode == null || tentativeG < neighborNode.g) {
                    if (neighborNode != null) {
                        context.openSet.remove(neighborNode);
                    }
                    neighborNode = new AStarNode(neighbor, current, tentativeG, heuristicDistance(neighbor, goal));
                    context.allNodes.put(neighborKey, neighborNode);
                    context.openSet.offer(neighborNode);
                }
            }
        }
        return null;
    }

    private static void executeMove(BattleUnit mover, HexCoord destination,
                                    List<BattleLogEntryDto> log, double currentTime,
                                    Map<String, BattleUnit> occupiedCells) {
        if (mover == null || destination == null) return;

        int startQ = mover.getQ();
        int startR = mover.getR();
        String oldPositionKey = mover.getPositionKey();

        if (occupiedCells != null) {
            occupiedCells.remove(oldPositionKey);
        }
        mover.move(destination.q, destination.r);
        if (occupiedCells != null) {
            occupiedCells.put(mover.getPositionKey(), mover);
        }

        Map<String, Object> moveData = new HashMap<>();
        moveData.put("fromQ", startQ);
        moveData.put("fromR", startR);
        moveData.put("toQ", destination.q);
        moveData.put("toR", destination.r);
        moveData.put("targetId", "not_needed");
        moveData.put("time", currentTime);
        moveData.put("duration", ACTION_DURATION);
        log.add(new BattleLogEntryDto(BattleLogEntryDto.LogEntryType.MOVE, mover.getId(), moveData));

        HexCoord goal = mover.getPathGoal();
        if (goal != null && mover.getPositionKey().equals(goal.toKey())) {
            mover.clearPath();
            mover.setNextActionTime(currentTime + 0.01);
        } else {
            mover.advancePath();
        }
    }

    private void removeDeadUnits(List<BattleUnit> teamList, List<BattleUnit> allUnitsList, Map<String, BattleUnit> occupiedCells) {
        if (teamList == null || allUnitsList == null) return;

        List<BattleUnit> deadUnitsInTeam = new ArrayList<>();
        Iterator<BattleUnit> iterator = teamList.iterator();
        while (iterator.hasNext()) {
            BattleUnit unit = iterator.next();
            if (unit.isDead()) {
                deadUnitsInTeam.add(unit);
                if (occupiedCells != null) {
                    occupiedCells.remove(unit.getPositionKey());
                }
                iterator.remove();
            }
        }
        allUnitsList.removeAll(deadUnitsInTeam);
    }

    private static void performAttack(BattleUnit attacker, BattleUnit target, List<BattleLogEntryDto> log, double currentTime) {
        if (attacker == null || target == null) return;
        if (target.isDead()) return;

        int minAtk = Math.min(attacker.getMinAttack(), attacker.getMaxAttack());
        int maxAtk = Math.max(attacker.getMinAttack(), attacker.getMaxAttack());
        int attackRange = maxAtk - minAtk + 1;
        int baseDamage = attackRange > 0 ? random.nextInt(attackRange) + minAtk : minAtk;
        int actualDamage = Math.max(0, baseDamage - target.getArmor());
        target.takeDamage(actualDamage);
        boolean isKill = target.isDead();

        // ВАЖНО: Фиксируем цель при атаке
        attacker.setCurrentTarget(target);
        attacker.setLastTargetSwitchTime(currentTime);

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
            // Сбрасываем цель только при убийстве
            attacker.setCurrentTarget(null);

            Map<String, Object> deathData = new HashMap<>();
            deathData.put("killerId", attacker.getId());
            deathData.put("time", currentTime);
            log.add(new BattleLogEntryDto(BattleLogEntryDto.LogEntryType.DEATH, target.getId(), deathData));
        }
    }

    private BattleUnit findOptimalTarget(BattleUnit attacker, List<BattleUnit> allTargets, BattleUnit targetToIgnore) {
        return allTargets.stream()
                .filter(t -> !t.isDead() && (targetToIgnore == null || !t.getId().equals(targetToIgnore.getId())))
                .min(Comparator.comparingInt((BattleUnit t) -> calculateDistance(attacker, t))
                        .thenComparingInt(BattleUnit::getCurrentHp))
                .orElse(null);
    }

    private int calculateAttackDistance(int q1, int r1, int q2, int r2) {
        int s1 = -q1 - r1;
        int s2 = -q2 - r2;
        return Math.max(Math.max(Math.abs(q1 - q2), Math.abs(r1 - r2)), Math.abs(s1 - s2));
    }

    private int calculateAttackDistance(BattleUnit a, BattleUnit b) {
        return calculateAttackDistance(a.getQ(), a.getR(), b.getQ(), b.getR());
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
        TeamType winnerTeam = (allies == null || allies.isEmpty()) ? TeamType.ENEMY : TeamType.ALLY;

        List<String> allyLossesIds = (initialPlacement == null) ? Collections.emptyList()
                : initialPlacement.stream()
                .filter(p -> p.getTeam() == TeamType.ALLY && (allies == null || allies.stream().noneMatch(u -> u.getId().equals(p.getId()))))
                .map(BattlePirateDto::getId).collect(Collectors.toList());

        List<String> enemyLossesIds = (initialPlacement == null) ? Collections.emptyList()
                : initialPlacement.stream()
                .filter(p -> p.getTeam() == TeamType.ENEMY && (enemies == null || enemies.stream().noneMatch(u -> u.getId().equals(p.getId()))))
                .map(BattlePirateDto::getId).collect(Collectors.toList());

        BattleResultDto.RewardsDto rewards;
        if (winnerTeam == TeamType.ALLY && quest != null) {
            rewards = BattleResultDto.RewardsDto.builder()
                    .experience(quest.getExpReward() != null ? quest.getExpReward() : 0L)
                    .gold(quest.getGoldReward() != null ? quest.getGoldReward() : 0L)
                    .wood(quest.getWoodReward() != null ? quest.getWoodReward() : 0L)
                    .stone(quest.getStoneReward() != null ? quest.getStoneReward() : 0L)
                    .items(quest.getItemRewards() == null ? Collections.emptyList() :
                            quest.getItemRewards().stream()
                                    .map(r -> new ItemRewardDto(r.getItem().getItemKey(), r.getItem().getName(), r.getItem().getImageUrl(), r.getQuantity()))
                                    .collect(Collectors.toList()))
                    .build();
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

        private List<HexCoord> savedPath;
        private int pathIndex;
        private HexCoord pathGoal;
        private BattleUnit currentTarget;

        private int pathBlockedAttempts = 0;

        private double lastTargetSwitchTime = -1.0;

        public BattleUnit(BattlePirateDto dto) {
            super(dto.getId(), dto.getTeam(), dto.getHp(), dto.getMinAttack(), dto.getMaxAttack(), dto.getArmor(), dto.getXp(), dto.getQ(), dto.getR(), dto.getImageId(), dto.getMovement(), dto.getAttackSpeed(), dto.getRange());
            this.currentHp = dto.getHp();
            this.range = dto.getRange() > 0 ? dto.getRange() : 1;
            this.positionKey = dto.getQ() + ":" + dto.getR();
            this.nextActionTime = 0.0;
            this.savedPath = null;
            this.pathIndex = 0;
            this.pathGoal = null;
            this.currentTarget = null;
        }

        public void incrementPathBlockedAttempts() {
            this.pathBlockedAttempts++;
        }

        public void setPath(List<HexCoord> path, HexCoord goal) {
            if (path == null || path.isEmpty()) {
                clearPath();
                return;
            }
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
            return savedPath != null && pathIndex + 1 < savedPath.size();
        }

        public HexCoord getNextStepFromPath() {
            if (hasPath()) {
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