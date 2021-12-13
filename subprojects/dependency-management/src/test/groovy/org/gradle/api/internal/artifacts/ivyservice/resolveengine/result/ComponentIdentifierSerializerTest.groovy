/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.api.internal.artifacts.ivyservice.resolveengine.result

import org.gradle.api.artifacts.component.LibraryBinaryIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.internal.artifacts.DefaultBuildIdentifier
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.internal.artifacts.DefaultProjectComponentIdentifier
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import org.gradle.internal.component.local.model.DefaultLibraryBinaryIdentifier
import org.gradle.internal.component.local.model.OpaqueComponentArtifactIdentifier
import org.gradle.internal.serialize.SerializerSpec
import org.gradle.util.Path

class ComponentIdentifierSerializerTest extends SerializerSpec {
    ComponentIdentifierSerializer serializer = new ComponentIdentifierSerializer()

    def "throws exception if null is provided"() {
        when:
        serialize(null, serializer)

        then:
        Throwable t = thrown(IllegalArgumentException)
        t.message == 'Provided component identifier may not be null'
    }

    def "serializes ModuleComponentIdentifier"() {
        given:
        ModuleComponentIdentifier identifier = new DefaultModuleComponentIdentifier(DefaultModuleIdentifier.newId('group-one', 'name-one'), 'version-one')

        when:
        ModuleComponentIdentifier result = serialize(identifier, serializer)

        then:
        result.group == 'group-one'
        result.module == 'name-one'
        result.version == 'version-one'
    }

    def "serializes LibraryIdentifier"() {
        given:
        LibraryBinaryIdentifier identifier = new DefaultLibraryBinaryIdentifier(':project', 'lib', 'variant')

        when:
        LibraryBinaryIdentifier result = serialize(identifier, serializer)

        then:
        result.projectPath == ':project'
        result.libraryName == 'lib'
    }

    def "serializes root ProjectComponentIdentifier"() {
        given:
        def identifier = new DefaultProjectComponentIdentifier(new DefaultBuildIdentifier("build"), Path.ROOT, Path.ROOT, "someProject")

        when:
        def result = serialize(identifier, serializer)

        then:
        result.identityPath == identifier.identityPath
        result.projectPath == identifier.projectPath
        result.projectPath() == identifier.projectPath()
        result.projectName == identifier.projectName
    }

    def "serializes root build ProjectComponentIdentifier"() {
        given:
        def identifier = new DefaultProjectComponentIdentifier(new DefaultBuildIdentifier("build"), Path.path(":a:b"), Path.path(":a:b"), "b")

        when:
        def result = serialize(identifier, serializer)

        then:
        result.identityPath == identifier.identityPath
        result.projectPath == identifier.projectPath
        result.projectPath() == identifier.projectPath()
        result.projectName == identifier.projectName
    }

    def "serializes other build root ProjectComponentIdentifier"() {
        given:
        def identifier = new DefaultProjectComponentIdentifier(new DefaultBuildIdentifier("build"), Path.path(":prefix:someProject"), Path.ROOT, "someProject")

        when:
        def result = serialize(identifier, serializer)

        then:
        result.identityPath == identifier.identityPath
        result.projectPath == identifier.projectPath
        result.projectPath() == identifier.projectPath()
        result.projectName == identifier.projectName
    }

    def "serializes other build ProjectComponentIdentifier"() {
        given:
        def identifier = new DefaultProjectComponentIdentifier(new DefaultBuildIdentifier("build"), Path.path(":prefix:a:b"), Path.path(":a:b"), "b")

        when:
        def result = serialize(identifier, serializer)

        then:
        result.identityPath == identifier.identityPath
        result.projectPath == identifier.projectPath
        result.projectPath() == identifier.projectPath()
        result.projectName == identifier.projectName
    }

    def "serialize OpaqueComponentArtifactIdentifier"() {
        given:
        def file = new File("example-1.0.jar")
        def identifier = new OpaqueComponentArtifactIdentifier(file)

        when:
        def result = serialize(identifier, serializer)

        then:
        result.displayName == "example-1.0.jar"
        result.file == file
        result.componentIdentifier == identifier
        result == identifier
    }
}
