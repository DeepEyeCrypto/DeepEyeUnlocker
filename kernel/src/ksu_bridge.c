#include "../include/deepeye_kernel.h"
#include <linux/fs.h>
#include <linux/kernel.h>
#include <linux/module.h>
#include <linux/mutex.h>
#include <linux/sched.h>
#include <linux/slab.h>
#include <linux/string.h>
#include <linux/uaccess.h>

/*
 * DeepEye Root Hiding Meta-Data
 * We store a list of target PIDs that we want to hide root from.
 */
#define MAX_TARGET_APPS 32

struct deepeye_config {
  int target_pids[MAX_TARGET_APPS];
  int target_count;
  struct mutex lock;
};

static struct deepeye_config global_config;

/**
 * is_target_process - Checks if the current task should be hidden
 */
static bool is_target_process(struct task_struct *task) {
  int i;
  bool found = false;

  mutex_lock(&global_config.lock);
  for (i = 0; i < global_config.target_count; i++) {
    if (task->tgid == global_config.target_pids[i]) {
      found = true;
      break;
    }
  }
  mutex_unlock(&global_config.lock);

  return found;
}

/**
 * deepeye_check_path_hiding - Represents a path hiding hook
 */
int deepeye_check_path_hiding(const char *path) {
  if (is_target_process(current)) {
    if (strstr(path, "/su") || strstr(path, "/magisk") || strstr(path, "ksu")) {
      return -ENOENT; // Force 'File Not Found'
    }
  }
  return 0; // Allow access
}

/**
 * deepeye_add_target_pid - Register a PID to hide root from
 */
void deepeye_add_target_pid(int pid) {
  mutex_lock(&global_config.lock);
  if (global_config.target_count < MAX_TARGET_APPS) {
    global_config.target_pids[global_config.target_count++] = pid;
    pr_info("DeepEye: Added PID %d to stealth list\n", pid);
  }
  mutex_unlock(&global_config.lock);
}

int deepeye_init_ksu(void) {
  pr_info("DeepEye: Initializing KSU Bridge Logic\n");
  memset(&global_config, 0, sizeof(global_config));
  mutex_init(&global_config.lock);

  return 0;
}

void deepeye_exit_ksu(void) {
  pr_info("DeepEye: Cleaning up KSU Bridge Logic\n");
}
