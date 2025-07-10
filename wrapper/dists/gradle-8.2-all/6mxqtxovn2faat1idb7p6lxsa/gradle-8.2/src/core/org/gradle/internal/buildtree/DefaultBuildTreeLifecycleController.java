/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.internal.buildtree;

import org.gradle.api.internal.GradleInternal;
import org.gradle.api.internal.SettingsInternal;
import org.gradle.execution.EntryTaskSelector;
import org.gradle.internal.Describables;
import org.gradle.internal.build.BuildLifecycleController;
import org.gradle.internal.build.ExecutionResult;
import org.gradle.internal.model.StateTransitionController;
import org.gradle.internal.model.StateTransitionControllerFactory;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class DefaultBuildTreeLifecycleController implements BuildTreeLifecycleController {
    private enum State implements StateTransitionController.State {
        NotStarted, Complete
    }

    private final BuildLifecycleController buildLifecycleController;
    private final BuildTreeWorkController workController;
    private final BuildTreeModelCreator modelCreator;
    private final BuildTreeFinishExecutor finishExecutor;
    private final StateTransitionController<State> state;

    public DefaultBuildTreeLifecycleController(
        BuildLifecycleController buildLifecycleController,
        BuildTreeWorkController workController,
        BuildTreeModelCreator modelCreator,
        BuildTreeFinishExecutor finishExecutor,
        StateTransitionControllerFactory controllerFactory
    ) {
        this.buildLifecycleController = buildLifecycleController;
        this.workController = workController;
        this.modelCreator = modelCreator;
        this.finishExecutor = finishExecutor;
        this.state = controllerFactory.newController(Describables.of("build tree state"), State.NotStarted);
    }

    @Override
    public void beforeBuild(Consumer<? super GradleInternal> action) {
        state.inState(State.NotStarted, () -> action.accept(buildLifecycleController.getGradle()));
    }

    @Override
    public void scheduleAndRunTasks() {
        scheduleAndRunTasks(null);
    }

    @Override
    public void scheduleAndRunTasks(EntryTaskSelector selector) {
        runBuild(() -> workController.scheduleAndRunRequestedTasks(selector));
    }

    @Override
    public <T> T fromBuildModel(boolean runTasks, BuildTreeModelAction<? extends T> action) {
        return runBuild(() -> {
            modelCreator.beforeTasks(action);
            if (runTasks) {
                ExecutionResult<Void> result = workController.scheduleAndRunRequestedTasks(null);
                if (!result.getFailures().isEmpty()) {
                    return result.asFailure();
                }
            }
            T model = modelCreator.fromBuildModel(action);
            return ExecutionResult.succeeded(model);
        });
    }

    @Override
    public <T> T withEmptyBuild(Function<? super SettingsInternal, T> action) {
        return runBuild(() -> {
            T result = buildLifecycleController.withSettings(action);
            return ExecutionResult.succeeded(result);
        });
    }

    private <T> T runBuild(Supplier<ExecutionResult<? extends T>> action) {
        return state.transition(State.NotStarted, State.Complete, () -> {
            ExecutionResult<? extends T> result;
            try {
                result = action.get();
            } catch (Throwable t) {
                result = ExecutionResult.failed(t);
            }

            RuntimeException finalReportableFailure = finishExecutor.finishBuildTree(result.getFailures());
            if (finalReportableFailure != null) {
                throw finalReportableFailure;
            }

            return result.getValue();
        });
    }
}
