/*
 * Copyright 2022 the original author or authors.
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

package org.gradle.api.tasks.diagnostics;

import org.gradle.api.Incubating;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.diagnostics.internal.ProjectDetails;
import org.gradle.internal.serialization.Cached;
import org.gradle.work.DisableCachingByDefault;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The base class for all Project based project report tasks.
 *
 * @param <T> The report model type
 * @since 7.6
 */
@Incubating
@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
public abstract class AbstractProjectBasedReportTask<T> extends ConventionReportTask {

    private final Cached<ProjectBasedReportModel<T>> reportModels = Cached.of(this::calculateReportModel);

    private ProjectBasedReportModel<T> calculateReportModel() {
        Map<ProjectDetails, T> map = new LinkedHashMap<>();
        for (Project project : getProjects()) {
            map.put(
                ProjectDetails.of(project),
                calculateReportModelFor(project)
            );
        }
        return new ProjectBasedReportModel<>(map);
    }

    protected abstract T calculateReportModelFor(Project project);

    protected abstract void generateReportFor(ProjectDetails project, T model);

    @TaskAction
    void action() {
        reportGenerator().generateReport(
            reportModels.get().modelsByProjectDetails.entrySet(),
            Map.Entry::getKey,
            entry -> {
                generateReportFor(entry.getKey(), entry.getValue());
                logClickableOutputFileUrl();
            }
        );
    }

    private static class ProjectBasedReportModel<T> {
        private final Map<ProjectDetails, T> modelsByProjectDetails;

        private ProjectBasedReportModel(Map<ProjectDetails, T> modelsByProjectDetails) {
            this.modelsByProjectDetails = modelsByProjectDetails;
        }
    }
}
