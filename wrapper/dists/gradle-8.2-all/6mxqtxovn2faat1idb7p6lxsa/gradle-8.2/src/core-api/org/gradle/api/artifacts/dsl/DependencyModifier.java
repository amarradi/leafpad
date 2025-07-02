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

package org.gradle.api.artifacts.dsl;

import org.gradle.api.Incubating;
import org.gradle.api.NonExtensible;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderConvertible;

import javax.inject.Inject;

/**
 * A {@code DependencyModifier} defines how to modify a dependency inside a custom {@code dependencies} block to select a different variant.
 *
 * @apiNote
 * Gradle has specific extensions to make explicit calls to {@code modify(...)} unnecessary from the DSL.
 * <ul>
 * <li>For Groovy DSL, we create {@code call(...)} equivalents for all the {@code modify(...)} methods.</li>
 * <li>For Kotlin DSL, we create {@code invoke(...)} equivalents for all the {@code modify(...)} methods.</li>
 * </ul>
 *
 * @implSpec The default implementation of all methods should not be overridden except {@link #modify(ModuleDependency)}. This method must be implemented.
 *
 * @implNote All other implementations of {@code modify(...)} delegate to {@link #modify(ModuleDependency)}.
 * <p>
 * Changes to this interface may require changes to the
 * {@link org.gradle.api.internal.artifacts.dsl.dependencies.DependenciesExtensionModule extension module for Groovy DSL} or
 * {@link org.gradle.kotlin.dsl.DependenciesExtensions extension functions for Kotlin DSL}.
 *
 * @since 8.0
 */
@Incubating
@NonExtensible
@SuppressWarnings("JavadocReference")
public interface DependencyModifier {
    /**
     * A dependency factory is used to convert supported dependency notations into {@link org.gradle.api.artifacts.Dependency} instances.
     *
     * @return a dependency factory
     * @implSpec Do not implement this method. Gradle generates the implementation automatically.
     *
     * @see DependencyFactory
     */
    @Inject
    DependencyFactory getDependencyFactory();

    /**
     * Create an {@link ExternalModuleDependency} from the given notation and modifies it to select the variant of the given module as described in {@link #modify(ModuleDependency)}.
     *
     * @param dependencyNotation the dependency notation
     * @return the modified dependency
     * @see DependencyFactory#create(CharSequence)
     */
    default ExternalModuleDependency modify(CharSequence dependencyNotation) {
        return modify(getDependencyFactory().create(dependencyNotation));
    }

    /**
     * Takes a given {@code Provider} to a {@link MinimalExternalModuleDependency} and modifies the dependency to select the variant of the given module as described in {@link #modify(ModuleDependency)}.
     *
     * @param providerConvertibleToDependency the provider
     * @return a provider to the modified dependency
     */
    default Provider<? extends MinimalExternalModuleDependency> modify(ProviderConvertible<? extends MinimalExternalModuleDependency> providerConvertibleToDependency) {
        return providerConvertibleToDependency.asProvider().map(this::modify);
    }

    /**
     * Takes a given {@code Provider} to a {@link ExternalModuleDependency} and modifies the dependency to select the variant of the given module as described in {@link #modify(ModuleDependency)}.
     *
     * @param providerToDependency the provider
     * @return a provider to the modified dependency
     */
    default <D extends ModuleDependency> Provider<D> modify(Provider<D> providerToDependency) {
        return providerToDependency.map(this::modify);
    }

    /**
     * Takes a given {@link ModuleDependency} and modifies the dependency to select the variant of the given module. Dependency resolution may fail if the given module does not have a compatible variant.
     * <p><br></p>
     *
     * @param dependency the dependency to modify
     * @return the modified dependency
     * @param <D> the type of the {@link ModuleDependency}
     * @implSpec This method must be implemented.
     */
    <D extends ModuleDependency> D modify(D dependency);
}
