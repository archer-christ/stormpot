/*
 * Copyright © 2011-2018 Chris Vest (mr.chrisvest@gmail.com)
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
package stormpot;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class QSlot<T extends Poolable> implements Slot, SlotInfo<T> {
  private final BlockingQueue<QSlot<T>> live;
  private final AtomicInteger poisonedSlots;
  final AtomicBoolean claimed;
  T obj;
  Exception poison;
  long created;
  long claims;
  long stamp;
  boolean expired;

  public QSlot(BlockingQueue<QSlot<T>> live, AtomicInteger poisonedSlots) {
    this.live = live;
    this.poisonedSlots = poisonedSlots;
    this.claimed = new AtomicBoolean(true);
  }
  
  public void claim() {
    claimed.set(true);
    claims++;
  }

  public void release(Poolable obj) {
    if (claimed.compareAndSet(true, false)) {
      if (expired) {
        poisonedSlots.getAndIncrement();
      }
      live.offer(this);
    }
  }

  @Override
  public void expire(Poolable obj) {
    expired = true;
  }

  @Override
  public long getAgeMillis() {
    return System.currentTimeMillis() - created;
  }

  @Override
  public long getClaimCount() {
    return claims;
  }

  @Override
  public T getPoolable() {
    return obj;
  }

  @Override
  public long getStamp() {
    return stamp;
  }

  @Override
  public void setStamp(long stamp) {
    this.stamp = stamp;
  }
}
