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
 * In a real scenario, this would check if the process belongs to
 * a banking app or a security-sensitive package.
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
 * deepeye_check_path_hiding - Sycall hook entry point (Simulated)
 * This would be called from hooked openat/stat syscalls.
 */
int deepeye_check_path_hiding(const char *path) {
  if (is_target_process(current)) {
    // Hide known root indicators
    if (strstr(path, "/su") || strstr(path, "/magisk") || strstr(path, "ksu") ||
        strstr(path, "busybox")) {
      pr_debug("DeepEye: Stealth block on path: %s\n", path);
      return -ENOENT; // Force 'File Not Found'
    }

    // Hide our own device node
    if (strstr(path, "/dev/deepeye")) {
      return -ENOENT;
    }
  }
  return 0; // Allow access
}

/**
 * deepeye_hide_from_proc - Filter out root processes from /proc
 * (Simulated logic for readdir hooks)
 */
bool deepeye_should_hide_proc(int pid) {
  // Hide Magisk/KSU/DeepEye specific PIDs from target apps
  if (is_target_process(current)) {
    // Logic to identify root management PIDs
    return true;
  }
  return false;
}

/**
 * deepeye_cloak_module - Remove LKM from list for stealth
 */
void deepeye_cloak_module(void) {
  // This is the classic LKM cloaking technique
  // list_del_init(&THIS_MODULE->list);
  // kobject_del(&THIS_MODULE->mkobj.kobj);
  pr_info("DeepEye: LKM has entered stealth mode (cloaked)\n");
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
  pr_info("DeepEye: Initializing KSU Bridge Logic v4.0\n");
  memset(&global_config, 0, sizeof(global_config));
  mutex_init(&global_config.lock);

  return 0;
}

void deepeye_exit_ksu(void) {
  pr_info("DeepEye: Cleaning up KSU Bridge Logic\n");
}
