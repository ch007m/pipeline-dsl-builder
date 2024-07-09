package dev.snowdrop.dsl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PipelineDSL {

    private String apiVersion;
    private String kind;
    private Metadata metadata;
    private Spec spec;

    public static PipelineDSL create() {
        return new PipelineDSL();
    }

    public PipelineDSL apiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    public PipelineDSL kind(String kind) {
        this.kind = kind;
        return this;
    }

    public Metadata metadata() {
        this.metadata = new Metadata(this);
        return this.metadata;
    }

    public Spec spec() {
        this.spec = new Spec(this);
        return this.spec;
    }

    @Override
    public String toString() {
        // Use SnakeYAML to convert this object to YAML
        // We'll handle this in the main method
        return "";
    }

    public static class Metadata {
        private final PipelineDSL parent;
        private String name;

        public Metadata(PipelineDSL parent) {
            this.parent = parent;
        }

        public Metadata name(String name) {
            this.name = name;
            return this;
        }

        public PipelineDSL endMetadata() {
            return parent;
        }
    }

    public static class Spec {
        private final PipelineDSL parent;
        private List<Workspace> workspaces = new ArrayList<>();
        private List<Param> params = new ArrayList<>();
        private List<Result> results = new ArrayList<>();
        private List<Task> tasks = new ArrayList<>();
        private List<Finally> finallyTasks = new ArrayList<>();

        public Spec(PipelineDSL parent) {
            this.parent = parent;
        }

        public Spec workspaces(List<Workspace> workspaces) {
            this.workspaces = workspaces;
            return this;
        }

        public Workspace workspace() {
            Workspace workspace = new Workspace(this);
            workspaces.add(workspace);
            return workspace;
        }

        public Spec params(List<Param> params) {
            this.params = params;
            return this;
        }

        public Param param() {
            Param param = new Param(this);
            params.add(param);
            return param;
        }

        public Spec results(List<Result> results) {
            this.results = results;
            return this;
        }

        public Result result() {
            Result result = new Result(this);
            results.add(result);
            return result;
        }

        public Spec tasks(List<Task> tasks) {
            this.tasks = tasks;
            return this;
        }

        public Task task() {
            Task task = new Task(this);
            tasks.add(task);
            return task;
        }

        public Spec finallyTasks(List<Finally> finallyTasks) {
            this.finallyTasks = finallyTasks;
            return this;
        }

        public Finally finallyTask() {
            Finally finallyTask = new Finally(this);
            finallyTasks.add(finallyTask);
            return finallyTask;
        }

        public PipelineDSL endSpec() {
            return parent;
        }
    }

    public static class Workspace {
        private final Spec parent;
        private String name;
        private Boolean optional;

        public Workspace(Spec parent) {
            this.parent = parent;
        }

        public Workspace name(String name) {
            this.name = name;
            return this;
        }

        public Workspace optional(Boolean optional) {
            this.optional = optional;
            return this;
        }

        public Spec endWorkspace() {
            return parent;
        }
    }

    public static class Param {
        private final Spec parent;
        private String description;
        private String name;
        private String type;
        private String defaultValue;

        public Param(Spec parent) {
            this.parent = parent;
        }

        public Param(TaskRef taskRef) {
        }

        public Param description(String description) {
            this.description = description;
            return this;
        }

        public Param name(String name) {
            this.name = name;
            return this;
        }

        public Param type(String type) {
            this.type = type;
            return this;
        }

        public Param defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Spec endParam() {
            return parent;
        }
    }

    public static class Result {
        private final Spec parent;
        private String description;
        private String name;
        private String value;

        public Result(Spec parent) {
            this.parent = parent;
        }

        public Result description(String description) {
            this.description = description;
            return this;
        }

        public Result name(String name) {
            this.name = name;
            return this;
        }

        public Result value(String value) {
            this.value = value;
            return this;
        }

        public Spec endResult() {
            return parent;
        }
    }

    public static class Task {
        private final Spec parent;
        private String name;
        private List<String> runAfter = new ArrayList<>();
        private List<Param> params = new ArrayList<>();
        private TaskRef taskRef;

        public Task(Spec parent) {
            this.parent = parent;
        }

        public Task name(String name) {
            this.name = name;
            return this;
        }

        public Task runAfter(String... runAfter) {
            this.runAfter.addAll(Arrays.asList(runAfter));
            return this;
        }

        public Task params(List<Param> params) {
            this.params = params;
            return this;
        }

        public Param param() {
            Param param = new Param();
            params.add(param);
            return param;
        }

        public Task taskRef(TaskRef params) {
            this.taskRef = taskRef;
            return this;
        }

        public Spec endTask() {
            return parent;
        }
    }

    public static class Finally {
        private final Spec parent;
        private String name;
        private List<When> when = new ArrayList<>();
        private List<Param> params = new ArrayList<>();
        private TaskRef taskRef;
        private List<WorkspaceBinding> workspaces = new ArrayList<>();

        public Finally(Spec parent) {
            this.parent = parent;
        }

        public Finally name(String name) {
            this.name = name;
            return this;
        }

        public Finally when(List<When> when) {
            this.when = when;
            return this;
        }

        public When when() {
            When when = new When(this);
            this.when.add(when);
            return when;
        }

        public Finally params(List<Param> params) {
            this.params = params;
            return this;
        }

        public Param param() {
            Param param = new Param(this);
            params.add(param);
            return param;
        }

        public Finally taskRef(TaskRef taskRef) {
            this.taskRef = taskRef;
            return this;
        }

        public TaskRef taskRef() {
            this.taskRef = new TaskRef();
            return this.taskRef;
        }

        public Finally workspaces(List<WorkspaceBinding> workspaces) {
            this.workspaces = workspaces;
            return this;
        }

        public WorkspaceBinding workspace() {
            WorkspaceBinding workspace = new WorkspaceBinding(this);
            workspaces.add(workspace);
            return workspace;
        }

        public Spec endFinally() {
            return parent;
        }
    }

    public static class When {
        private final Finally parent;
        private String input;
        private String operator;
        private List<String> values = new ArrayList<>();

        public When(Finally parent) {
            this.parent = parent;
        }

        public When input(String input) {
            this.input = input;
            return this;
        }

        public When operator(String operator) {
            this.operator = operator;
            return this;
        }

        public When values(String... values) {
            this.values.addAll(Arrays.asList(values));
            return this;
        }

        public Finally endWhen() {
            return parent;
        }
    }

    public static class TaskRef {
        private Task parent;
        private String resolver;
        private List<Param> params = new ArrayList<>();

        public TaskRef() {}

        public TaskRef resolver(String resolver) {
            this.resolver = resolver;
            return this;
        }

        public TaskRef params(List<Param> params) {
            this.params = params;
            return this;
        }

        public Param param() {
            Param param = new Param(this);
            params.add(param);
            return param;
        }

        public Task endTaskRef() {
            return parent;
        }
    }

    public static class WorkspaceBinding {
        private final Finally parent;
        private String workspace;
        private String name;

        public WorkspaceBinding(Finally parent) {
            this.parent = parent;
        }

        public WorkspaceBinding workspace(String workspace) {
            this.workspace = workspace;
            return this;
        }

        public WorkspaceBinding name(String name) {
            this.name = name;
            return this;
        }

        public Finally endWorkspace() {
            return parent;
        }
    }
}
