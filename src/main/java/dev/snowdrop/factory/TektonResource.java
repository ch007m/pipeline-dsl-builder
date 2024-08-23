package dev.snowdrop.factory;

import dev.snowdrop.model.*;
import dev.snowdrop.model.ConfigMap;
import dev.snowdrop.model.Secret;
import dev.snowdrop.model.Volume;
import dev.snowdrop.service.FileUtilSvc;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.tekton.pipeline.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static dev.snowdrop.model.Action.STEP_SCRIPT_IMAGE;

public class TektonResource {

    private static final Logger logger = LoggerFactory.getLogger(TektonResource.class);

    public static HasMetadata create(Configurator cfg) {
        List<Action> actions = cfg.getJob().getActions();
        String domain = cfg.getDomain().toUpperCase();
        Type TYPE = Type.valueOf(cfg.getType().toUpperCase());

        if (TYPE == null) {
            throw new RuntimeException("Missing type/provider");
        }

        if (domain == null) {
            throw new RuntimeException("Missing domain");
        }

        return JobFactory
            .withType(TYPE)
            .generatePipeline(cfg);
    }

    public static PipelineTask createTaskWithEmbeddedScript(Action action, String runAfter, List<String> args, List<When> when, Map<String, Workspace> jobWorkspacesMap, List<TaskResult> results) {
        String embeddedScript;

        if (action.getScript() != null) {
            embeddedScript = action.getScript();
        } else if (action.getScriptFileUrl() != null) {
            try {
                embeddedScript = FileUtilSvc.fetchUrlRawContent(action.getScriptFileUrl());
            } catch (IOException e) {
                throw new RuntimeException("Cannot fetch the script file: " + action.getScriptFileUrl(), e);
            }
        } else {
            throw new RuntimeException("No embedded script configured");
        }

        if (action.isFinally()) {
            // No need to define a runAfter for finally. To be checked of course !
            runAfter = null;
        }

        PipelineTask pipelineTask = new PipelineTaskBuilder()
            // @formatter:off
            .withName(action.getName())
            .withRunAfter(runAfter != null ? Collections.singletonList(runAfter) : null)
            .withWhen().addToWhen(
                when.stream()
                    .map(w -> new WhenExpressionBuilder()
                        .withInput(w.getInput())
                        .withOperator(w.getOperator())
                        .withValues(w.getValues())
                        .build())
                    .toArray(WhenExpression[]::new))
            .withParams(action.getParams() != null ? populatePipelineParams(action.getParams()) : null)
            .withWorkspaces(populateTaskWorkspaces(action, jobWorkspacesMap, null))
            .withTaskSpec(
                new EmbeddedTaskBuilder()
                    .addNewStep()
                      .withName("run-script")
                      .withArgs(args)
                      .withImage(action.IMAGES.get(STEP_SCRIPT_IMAGE))
                      .withScript(embeddedScript)
                    .endStep()
                    .withResults(results)
                    .build())
            .build();
        // @formatter:on
        return pipelineTask;
    }

    public static List<WorkspacePipelineTaskBinding> populateTaskWorkspaces(Action action, Map<String, Workspace> jobWorkspacesMap, List<WorkspaceDeclaration> taskWorkspaces) {
        // List of workspaces to be generated by this method
        List<WorkspacePipelineTaskBinding> wksPipelineTask = new ArrayList<>();

        // Map of workspaces from Job and action (= task) using as key, the workspace's name
        Map<String, Workspace> wksMerged = new HashMap<>();

        // Create a Map of the action's workspaces using as key the workspace's name
        Map<String, Workspace> actionWorkspacesMap = Optional.ofNullable(action.getWorkspaces())
            .orElse(Collections.emptyList()) // Handle null case by providing an empty list
            .stream()
            .collect(Collectors.toMap(Workspace::getName, name -> name));

        wksMerged.putAll(jobWorkspacesMap);
        wksMerged.putAll(actionWorkspacesMap);

        // As the task referenced includes workspaces, we will try to match them with the job's workspaces
        if (taskWorkspaces != null && !taskWorkspaces.isEmpty()) {
            taskWorkspaces.stream().forEach(wks -> {
                String wksNameToSearch = wks.getName();

                if (wksMerged.containsKey(wksNameToSearch)) {
                    // Job's workspace and task's workspace matches
                    // We will now check if name
                    logger.info("Match found using as key: " + wksNameToSearch);

                    Workspace wksMatching = wksMerged.get(wksNameToSearch);

                    WorkspacePipelineTaskBindingBuilder wuBuilder = new WorkspacePipelineTaskBindingBuilder();
                    wuBuilder
                        .withName(wksMatching.getName())
                        .withWorkspace(wksMatching.getWorkspace() != null ? wksMatching.getWorkspace() : wksMatching.getName())
                        .build();
                    wksPipelineTask.add(wuBuilder.build());
                }
            });

        } else {
            // taskWorkspaces can be null - empty when we generate a Pipeline using TaskSpec and embedded script
            // In this case, we will check if wks have been defined at the level of the action
            // and if the wks exists within the Job's workspace
            actionWorkspacesMap.keySet().forEach(k -> {
                if (wksMerged.containsKey(k) && jobWorkspacesMap.containsKey(k)) {
                    // Job's workspace and task's workspace matches
                    // We will now check if name
                    logger.info("Match found using as key: " + k);

                    Workspace wksMatching = wksMerged.get(k);

                    WorkspacePipelineTaskBindingBuilder wuBuilder = new WorkspacePipelineTaskBindingBuilder();
                    wuBuilder
                        .withName(wksMatching.getName())
                        .withWorkspace(wksMatching.getWorkspace() != null ? wksMatching.getWorkspace() : wksMatching.getName())
                        .build();
                    wksPipelineTask.add(wuBuilder.build());
                } else {
                    throw new RuntimeException("The following workspace has not been defined at the job's level: " + k);
                }
            });

            return wksPipelineTask;
        }
        return wksPipelineTask;
    }

    public static List<When> populateWhenList(Action action) {
        List<When> whenList = new ArrayList<>();
        if (action.getWhen() != null && !action.getWhen().isEmpty()) {
            action.getWhen().forEach(w -> {
                String operator = "in";
                String[] res = w.split(":");
                whenList.add(
                    new When()
                        .input(res[0])
                        .operator(operator)
                        .values(List.of(res[1].trim()))
                );
            });
        }
        return whenList;
    }

    public static List<WorkspaceBinding> populatePipelineWorkspaces(List<Workspace> wks) {
        List<WorkspaceBinding> workspaceList = new ArrayList<>();
        for (Workspace wk : wks) {
            WorkspaceBindingBuilder binding = new WorkspaceBindingBuilder();
            binding.withName(wk.getName());

            if (wk.getVolumeClaimTemplate() != null) {
                Volume v = wk.getVolumeClaimTemplate();
                // @formatter:off
                binding.withVolumeClaimTemplate(
                    new PersistentVolumeClaimBuilder()
                        .editOrNewSpec()
                        .withResources(
                            new VolumeResourceRequirementsBuilder()
                                .addToRequests(
                                    v.STORAGE,
                                    new Quantity(v.getStorage()))
                                .build()
                        )
                        .addToAccessModes(v.getAccessMode())
                        .endSpec()
                        .build()
                );
                // @formatter:on
                workspaceList.add(binding.build());
            }

            // If volumes size > 0 than we assume that the user would like to use: Projected
            if (wk.getVolumeSources() != null && wk.getVolumeSources().size() > 0) {
                ProjectedVolumeSourceBuilder pvsb = new ProjectedVolumeSourceBuilder();
                for (Volume v : wk.getVolumeSources()) {
                    if (v.getSecret() != null) {
                        pvsb.addNewSource()
                            .withSecret(new SecretProjectionBuilder().withName(v.getSecret()).build())
                            .endSource();
                    }
                }
                binding.withProjected(pvsb.build());
                workspaceList.add(binding.build());
            }

            // If volume is a secret
            if (wk.getSecret() != null) {
                Secret secret = wk.getSecret();
                binding.withSecret(new SecretVolumeSourceBuilder()
                    .withSecretName(secret.getName())
                    .build());
                workspaceList.add(binding.build());
            }

            // If volume is a ConfigMap
            if (wk.getConfigMap() != null) {
                ConfigMap cm = wk.getConfigMap();
                binding.withSecret(new SecretVolumeSourceBuilder()
                    .withSecretName(cm.getName())
                    .build());
                workspaceList.add(binding.build());
            }
        }
        return workspaceList;
    }

    public static List<Param> populatePipelineParams(List<Map<String, Object>> params) {
        List<Param> paramList = new ArrayList<>();
        for (Map<String, Object> hash : params) {
            hash.forEach((key, val) -> {
                    ParamValue paramValue;
                    if (val instanceof String) {
                        paramValue = new ParamValue((String) val);
                    } else if (val instanceof Boolean) {
                        paramValue = new ParamValue(Boolean.toString((Boolean) val));
                    } else if (val instanceof List<?>) {
                        List<?> rawList = (List<?>) val;
                        List<String> stringList = rawList.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .collect(Collectors.toList());
                        paramValue = new ParamValue(stringList);
                    } else {
                        paramValue = new ParamValue(String.valueOf(val)); // Default to String representation
                    }

                    paramList.add(new ParamBuilder().withName(key).withValue(paramValue).build());
                }
            );
        }
        return paramList;
    }

    public static List<PipelineResult> populatePipelineResults(List<Map<String, String>> results) {
        List<PipelineResult> pipelineResultList = new ArrayList<>();

        for (Map<String, String> aMap : results) {
            aMap.forEach((key, val) -> {
                pipelineResultList.add(new PipelineResultBuilder()
                    .withName(key)
                    .withValue(new ParamValue(val))
                    .build());
            });
        }
        return pipelineResultList;
    }

    public static List<TaskResult> populateTaskResults(List<Map<String, String>> results) {
        List<TaskResult> taskResultList = new ArrayList<>();

        for (Map<String, String> aMap : results) {
            aMap.forEach((key, val) -> {
                taskResultList.add(new TaskResultBuilder()
                    .withName(key)
                    .withDescription(val)
                    .build());
            });
        }
        return taskResultList;
    }

    public static TimeoutFields populateTimeOut(String timeOut) {
        Duration duration = null;
        try {
            duration = Duration.parse(timeOut);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new TimeoutFieldsBuilder()
            .withPipeline(duration)
            .build();
    }
}
