/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.internal.watch.vfs;

import org.gradle.internal.operations.BuildOperationRunner;
import org.gradle.internal.service.scopes.Scopes;
import org.gradle.internal.service.scopes.ServiceScope;
import org.gradle.internal.vfs.VirtualFileSystem;
import org.gradle.internal.watch.registry.WatchMode;

import java.io.File;

/**
 * Controls the lifecycle and book-keeping for file system watching.
 */
@ServiceScope(Scopes.UserHome.class)
public interface BuildLifecycleAwareVirtualFileSystem extends VirtualFileSystem, FileSystemWatchingInformation {

    /**
     * Called when the build is started.
     *
     * @return whether watching the file system is currently enabled. This requires that the feature
     * is supported on the current operating system, it is enabled for the build, and has been successfully
     * started.
     */
    boolean afterBuildStarted(WatchMode watchingEnabled, VfsLogging vfsLogging, WatchLogging watchLogging, BuildOperationRunner buildOperationRunner);

    /**
     * Register a watchable hierarchy.
     *
     * Only locations within watchable hierarchies will be watched for changes.
     * This method is first called for the root directory of the root project.
     * It is also called for the root directories of included builds, and all other nested builds.
     */
    void registerWatchableHierarchy(File rootDirectoryForWatching);

    /**
     * Called when the build is finished.
     */
    void beforeBuildFinished(WatchMode watchMode, VfsLogging vfsLogging, WatchLogging watchLogging, BuildOperationRunner buildOperationRunner, int maximumNumberOfWatchedHierarchies);

}
