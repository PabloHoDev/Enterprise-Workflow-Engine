package com.pablohenrique.workflowengine.definition.infrastructure.persistence;

import com.pablohenrique.workflowengine.definition.application.DefinitionSummary;
import com.pablohenrique.workflowengine.definition.application.WorkflowDefinitionRepository;
import com.pablohenrique.workflowengine.definition.contract.TransitionSnapshot;
import com.pablohenrique.workflowengine.definition.contract.VersionSnapshot;
import com.pablohenrique.workflowengine.definition.domain.DefinitionVersion;
import com.pablohenrique.workflowengine.definition.domain.StateDefinition;
import com.pablohenrique.workflowengine.definition.domain.StateType;
import com.pablohenrique.workflowengine.definition.domain.TransitionDefinition;
import com.pablohenrique.workflowengine.definition.domain.VersionStatus;
import com.pablohenrique.workflowengine.definition.domain.WorkflowDefinition;
import com.pablohenrique.workflowengine.rules.contract.Rule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
class JpaWorkflowDefinitionRepository implements WorkflowDefinitionRepository {

    private final WorkflowDefinitionJpaRepository definitions;
    private final DefinitionVersionJpaRepository versions;

    JpaWorkflowDefinitionRepository(WorkflowDefinitionJpaRepository definitions,
                                    DefinitionVersionJpaRepository versions) {
        this.definitions = definitions;
        this.versions = versions;
    }

    @Override
    public void save(WorkflowDefinition definition) {
        WorkflowDefinitionEntity entity = definitions.findById(definition.id()).orElse(null);
        if (entity == null) {
            entity = new WorkflowDefinitionEntity(definition.id(), definition.key(), definition.name(),
                    definition.description(), definition.createdAt(), definition.updatedAt());
            definition.versions().stream().map(this::toEntity).forEach(entity::addVersion);
            definitions.save(entity);
            return;
        }

        // A estrutura de uma versão é imutável: apenas status muda e novas versões são acrescentadas.
        entity.setUpdatedAt(definition.updatedAt());
        Map<Integer, DefinitionVersionEntity> persisted = entity.getVersions().stream()
                .collect(Collectors.toMap(DefinitionVersionEntity::getNumber, Function.identity()));
        for (DefinitionVersion version : definition.versions()) {
            DefinitionVersionEntity persistedVersion = persisted.get(version.number());
            if (persistedVersion == null) {
                entity.addVersion(toEntity(version));
            } else {
                persistedVersion.setStatus(version.status());
            }
        }
    }

    @Override
    public boolean existsByKey(String key) {
        return definitions.existsByKey(key);
    }

    @Override
    public Optional<WorkflowDefinition> findByKey(String key) {
        return definitions.findByKey(key).map(this::toDomain);
    }

    @Override
    public Page<DefinitionSummary> findAll(Pageable pageable) {
        return definitions.findAll(pageable).map(entity -> new DefinitionSummary(entity.getId(), entity.getKey(),
                entity.getName(), entity.getDescription(), entity.getCreatedAt(), entity.getUpdatedAt()));
    }

    @Override
    public Optional<VersionSnapshot> findActiveVersionSnapshot(String definitionKey) {
        return versions.findByDefinitionKeyAndStatus(definitionKey, VersionStatus.ACTIVE).map(this::toSnapshot);
    }

    @Override
    public Optional<VersionSnapshot> findVersionSnapshot(UUID versionId) {
        return versions.findById(versionId).map(this::toSnapshot);
    }

    private DefinitionVersionEntity toEntity(DefinitionVersion version) {
        List<StateEmbeddable> states = version.states().stream()
                .map(state -> new StateEmbeddable(state.name(), state.type()))
                .toList();
        List<TransitionEntity> transitions = new ArrayList<>();
        for (TransitionDefinition transition : version.transitions()) {
            List<RuleEmbeddable> rules = transition.rules().stream()
                    .map(rule -> new RuleEmbeddable(rule.field(), rule.operator(), rule.expectedValue()))
                    .toList();
            transitions.add(new TransitionEntity(transitions.size(), transition.action(), transition.from(),
                    transition.to(), transition.requiredRole(), rules));
        }
        return new DefinitionVersionEntity(version.id(), version.number(), version.status(), version.createdAt(),
                states, transitions);
    }

    private WorkflowDefinition toDomain(WorkflowDefinitionEntity entity) {
        List<DefinitionVersion> domainVersions = entity.getVersions().stream()
                .map(version -> DefinitionVersion.restore(version.getId(), version.getNumber(), version.getStatus(),
                        version.getStates().stream()
                                .map(state -> new StateDefinition(state.getName(), state.getType()))
                                .toList(),
                        version.getTransitions().stream()
                                .map(transition -> new TransitionDefinition(transition.getAction(),
                                        transition.getFromState(), transition.getToState(),
                                        transition.getRequiredRole(), toRules(transition)))
                                .toList(),
                        version.getCreatedAt()))
                .toList();
        return WorkflowDefinition.restore(entity.getId(), entity.getKey(), entity.getName(), entity.getDescription(),
                entity.getCreatedAt(), entity.getUpdatedAt(), domainVersions);
    }

    private VersionSnapshot toSnapshot(DefinitionVersionEntity version) {
        String initialState = version.getStates().stream()
                .filter(state -> state.getType() == StateType.INITIAL)
                .map(StateEmbeddable::getName)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Version " + version.getId() + " has no INITIAL state"));
        Set<String> terminalStates = version.getStates().stream()
                .filter(state -> state.getType() == StateType.TERMINAL)
                .map(StateEmbeddable::getName)
                .collect(Collectors.toSet());
        List<TransitionSnapshot> transitions = version.getTransitions().stream()
                .map(transition -> new TransitionSnapshot(transition.getAction(), transition.getFromState(),
                        transition.getToState(), transition.getRequiredRole(), toRules(transition)))
                .toList();
        return new VersionSnapshot(version.getId(), version.getDefinition().getKey(), version.getNumber(),
                initialState, terminalStates, transitions);
    }

    private List<Rule> toRules(TransitionEntity transition) {
        return transition.getRules().stream()
                .map(rule -> new Rule(rule.getField(), rule.getOperator(), rule.getExpectedValue()))
                .toList();
    }
}
