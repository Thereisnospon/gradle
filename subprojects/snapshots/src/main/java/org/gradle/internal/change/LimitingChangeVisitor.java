/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.internal.change;
//超过 change 上限了就不继续访问
public class LimitingChangeVisitor implements ChangeVisitor {
    private final int maxReportedChanges;
    private final ChangeVisitor delegate;
    private int visited;

    public LimitingChangeVisitor(int maxReportedChanges, ChangeVisitor delegate) {
        this.maxReportedChanges = maxReportedChanges;
        this.delegate = delegate;
    }

    @Override
    public boolean visitChange(Change change) {
        boolean delegateResult = delegate.visitChange(change);
        visited++;
        return delegateResult && visited < maxReportedChanges;
    }
}
