/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.api.plugins.quality

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class WithoutIA extends AbstractIntegrationSpec {
    def setup(){
        buildFile<<'''
	    apply plugin:"java"
	    apply plugin:"pmd"
	    repositories { mavenCentral() }
	'''
    }
    def "incremental analysis can be enabled"() {
        given:
        goodCode()
        buildFile << 'pmd {  }'

        when:
        args('--info')
        succeeds("pmdMain")

        then:
        true

        when:
        args('--rerun-tasks', '--info')
        succeeds("pmdMain")

        then:
        true
    }

    def 'incremental analysis is transparent'() {
        given:
        buildFile << 'pmd {  }'
        goodCode()
        badCode()

        when:
        fails('pmdMain')

        then:
        true

        when:
        file('src/main/java/org/gradle/BadClass.java').delete()
        succeeds('pmdMain')

        then:
        true
    }

    private goodCode() {
        file("src/main/java/org/gradle/GoodClass.java") <<
            "package org.gradle; class GoodClass { public boolean isFoo(Object arg) { return true; } }"
    }

    private badCode() {
        // PMD Lvl 2 Warning BooleanInstantiation
        // PMD Lvl 3 Warning OverrideBothEqualsAndHashcode
        file("src/main/java/org/gradle/BadClass.java") <<
            "package org.gradle; class BadClass { public boolean equals(Object arg) { return java.lang.Boolean.valueOf(true); } }"
    }
}
