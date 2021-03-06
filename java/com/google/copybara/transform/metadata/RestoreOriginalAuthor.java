/*
 * Copyright (C) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.copybara.transform.metadata;

import com.google.copybara.Author;
import com.google.copybara.Change;
import com.google.copybara.NonReversibleValidationException;
import com.google.copybara.TransformWork;
import com.google.copybara.Transformation;
import com.google.copybara.ValidationException;
import com.google.copybara.util.console.Console;
import com.google.devtools.build.lib.syntax.EvalException;
import java.io.IOException;

/**
 * Restores an original author stored in a label.
 */
public class RestoreOriginalAuthor implements Transformation {

  private final String label;

  RestoreOriginalAuthor(String label) {
    this.label = label;
  }

  @Override
  public void transform(TransformWork work, Console console)
      throws IOException, ValidationException {
    Author author = null;
    // If multiple commits are included (for example on a squash for skipping a bad change),
    // last author wins.
    for (Change<?> change : work.getChanges().getCurrent()) {
      String labelValue = change.getLabels().get(label);
      if (labelValue != null) {
        try {
          author = Author.parse(/*location=*/null, labelValue);
        } catch (EvalException e) {
          // Don't fail the migration because the label is wrong since it is very
          // difficult for a user to recover from this.
          console.warn("Cannot restore original author: " + e.getMessage());
        }
      }
    }
    if (author != null) {
      work.setAuthor(author);
      work.removeLabel(label);
    }
  }

  @Override
  public Transformation reverse() throws NonReversibleValidationException {
    return new SaveOriginalAuthor(label);
  }

  @Override
  public String describe() {
    return "Restoring original author";
  }
}
