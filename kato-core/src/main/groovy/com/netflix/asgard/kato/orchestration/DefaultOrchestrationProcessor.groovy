/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.asgard.kato.orchestration

import com.netflix.asgard.kato.data.task.Task
import com.netflix.asgard.kato.data.task.TaskRepository
import groovy.util.logging.Log4j

import java.util.concurrent.TimeoutException

import static com.netflix.asgard.kato.holders.ApplicationContextHolder.applicationContext

@Log4j
class DefaultOrchestrationProcessor implements OrchestrationProcessor {
  private static final String TASK_PHASE = "ORCHESTRATION"
  private final Task task = TaskRepository.threadLocalTask.get()

  private final List<AtomicOperation> atomicOperations

  DefaultOrchestrationProcessor(List<AtomicOperation> atomicOperations) {
    this.atomicOperations = atomicOperations
  }

  @Override
  List process() {
    final List results = []

    for (AtomicOperation atomicOperation : atomicOperations) {
      task.updateStatus TASK_PHASE, "Processing op: ${atomicOperation.class.simpleName}"
      try {
        results << atomicOperation.operate(results)
      } catch (e) {
        log.error e
        task.updateStatus TASK_PHASE, "operation failed: ${atomicOperation.class.simpleName} -- ${e.message}"
        task.fail()
      }
    }

    results
  }

  static Task start(List<AtomicOperation> atomicOperations) {
    def task = taskRepository.create(TASK_PHASE, "Initializing Orchestration Task...")
    Thread.start {
      // Autowire the atomic operations
      atomicOperations.each { autowire it }
      TaskRepository.threadLocalTask.set(task)
      OrchestrationProcessor orchestrationProcessor = new DefaultOrchestrationProcessor(atomicOperations)
      try {
        orchestrationProcessor.process()
        task.updateStatus(TASK_PHASE, "Orchestration is complete.")
        task.complete()
      } catch (TimeoutException IGNORE) {
        task.updateStatus "INIT", "Orchestration timed out."
        task.fail()
        log.error "Timeout."
      }
    }
    task
  }

  static void autowire(obj) {
    applicationContext.autowireCapableBeanFactory.autowireBean obj
  }

  private static TaskRepository _taskRepository

  static TaskRepository getTaskRepository() {
    if (!_taskRepository) {
      _taskRepository = applicationContext.getBean(TaskRepository)
    }
    _taskRepository
  }
}