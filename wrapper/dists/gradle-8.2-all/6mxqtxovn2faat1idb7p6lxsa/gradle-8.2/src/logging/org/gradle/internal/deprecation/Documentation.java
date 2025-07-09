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

package org.gradle.internal.deprecation;

import com.google.common.base.Preconditions;
import org.gradle.api.internal.DocumentationRegistry;

import javax.annotation.Nullable;

import static org.gradle.api.internal.DocumentationRegistry.RECOMMENDATION;

public abstract class Documentation {
    private static final DocumentationRegistry DOCUMENTATION_REGISTRY = new DocumentationRegistry();

    static final Documentation NO_DOCUMENTATION = new NullDocumentation();

    public static Documentation userManual(String id, String section) {
        return new UserGuide(id, section);
    }

    static Documentation userManual(String id) {
        return new UserGuide(id, null);
    }

    static Documentation upgradeGuide(int majorVersion, String upgradeGuideSection) {
        return new UpgradeGuide(majorVersion, upgradeGuideSection);
    }

    public static Documentation dslReference(Class<?> targetClass, String property) {
        return new DslReference(targetClass, property);
    }

    @Nullable
    public abstract String documentationUrl();

    @Nullable
    public String consultDocumentationMessage() {
        return String.format(RECOMMENDATION, "information", documentationUrl());
    }

    public static abstract class AbstractBuilder<T> {
        protected abstract T withDocumentation(Documentation documentation);

        /**
         * Allows proceeding without including any documentation reference.
         * Consider using one of the documentation providing methods instead.
         */
        public T undocumented() {
            return withDocumentation(Documentation.NO_DOCUMENTATION);
        }

        /**
         * Output: See USER_MANUAL_URL for more details.
         */
        public T withUserManual(String documentationId) {
            return withDocumentation(Documentation.userManual(documentationId));
        }

        /**
         * Output: See USER_MANUAL_URL for more details.
         */
        public T withUserManual(String documentationId, String section) {
            return withDocumentation(Documentation.userManual(documentationId, section));
        }

        /**
         * Output: See DSL_REFERENCE_URL for more details.
         */
        public T withDslReference(Class<?> targetClass, String property) {
            return withDocumentation(Documentation.dslReference(targetClass, property));
        }

        /**
         * Output: Consult the upgrading guide for further information: UPGRADE_GUIDE_URL
         */
        public T withUpgradeGuideSection(int majorVersion, String upgradeGuideSection) {
            return withDocumentation(Documentation.upgradeGuide(majorVersion, upgradeGuideSection));
        }
    }

    private static class NullDocumentation extends Documentation {

        private NullDocumentation() {
        }

        @Override
        public String documentationUrl() {
            return null;
        }

        @Override
        public String consultDocumentationMessage() {
            return null;
        }
    }

    private static class UserGuide extends Documentation {
        private final String id;
        private final String section;

        private UserGuide(String id, @Nullable String section) {
            this.id = Preconditions.checkNotNull(id);
            this.section = section;
        }

        @Override
        public String documentationUrl() {
            if (section != null) {
                return DOCUMENTATION_REGISTRY.getDocumentationFor(id, section);
            }
            return DOCUMENTATION_REGISTRY.getDocumentationFor(id);
        }
    }

    private static class UpgradeGuide extends Documentation {
        private final int majorVersion;
        private final String section;

        private UpgradeGuide(int majorVersion, String section) {
            this.majorVersion = majorVersion;
            this.section = Preconditions.checkNotNull(section);
        }

        @Override
        public String documentationUrl() {
            return DOCUMENTATION_REGISTRY.getDocumentationFor("upgrading_version_" + majorVersion, section);
        }

        @Override
        public String consultDocumentationMessage() {
            return "Consult the upgrading guide for further information: " + documentationUrl();
        }
    }

    private static class DslReference extends Documentation {
        private final Class<?> targetClass;
        private final String property;

        public DslReference(Class<?> targetClass, String property) {
            this.targetClass = Preconditions.checkNotNull(targetClass);
            this.property = Preconditions.checkNotNull(property);
        }

        @Override
        public String documentationUrl() {
            return DOCUMENTATION_REGISTRY.getDslRefForProperty(targetClass, property);
        }
    }

}



