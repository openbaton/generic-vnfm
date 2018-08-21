/*
 * Copyright (c) 2015-2018 Open Baton (http://openbaton.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openbaton.vnfm.generic.model;

public class EmsRegistrationUnit {
  private boolean registered;
  private boolean canceled;
  private String value;

  public boolean isCanceled() {
    return canceled;
  }

  public void setCanceled(boolean canceled) {
    this.canceled = canceled;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public void setRegistered(boolean registered) {
    this.registered = registered;
  }

  public boolean isRegistered() {
    return registered;
  }

  /**
   * Block waiting for the EMS containing the id in the value field.
   *
   * @return true if registered in time
   * @throws InterruptedException if thread is interrupted
   */
  public EmsRegistrationUnit waitForEms(long timeout) throws InterruptedException {
    synchronized (this) {
      if (registered) return this;
      this.wait(timeout);
    }
    return this;
  }

  public void registerAndNotify() {
    synchronized (this) {
      this.registered = true;
      this.canceled = false;
      this.notify();
    }
  }

  public void cancelAndNotify() {
    synchronized (this) {
      this.canceled = true;
      this.registered = false;
      this.notify();
    }
  }

  @Override
  public String toString() {
    return "EmsRegistrationUnit{"
        + "registered="
        + registered
        + ", canceled="
        + canceled
        + ", value='"
        + value
        + '\''
        + '}';
  }
}
