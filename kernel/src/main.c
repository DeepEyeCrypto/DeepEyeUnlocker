#include "../include/deepeye_kernel.h"
#include <linux/device.h>
#include <linux/fs.h>
#include <linux/init.h>
#include <linux/kernel.h>
#include <linux/module.h>
#include <linux/uaccess.h>

// Use a custom prefix for logging to make it easy to grep
#define pr_fmt(fmt) "DeepEyeKernel: " fmt
#define DEVICE_NAME "deepeye"
#define CLASS_NAME "deepeye_class"

MODULE_LICENSE("GPL");
MODULE_AUTHOR("DeepEyeUnlocker");
MODULE_DESCRIPTION("DeepEyeUnlocker Kernel Bridge - Foundation");
MODULE_VERSION("4.0.0");

static int major_number;
static struct class *deepeye_class = NULL;
static struct device *deepeye_device = NULL;

/**
 * deepeye_ioctl - Handler for userspace commands
 */
static long deepeye_ioctl(struct file *file, unsigned int cmd,
                          unsigned long arg) {
  switch (cmd) {
  case DEEPEYE_HIDE_ROOT: {
    struct hide_root_args args;
    if (copy_from_user(&args, (void __user *)arg, sizeof(args)))
      return -EFAULT;

    pr_info("IOCTL: Hide root request for PID %d (%s)\n", args.pid,
            args.package_name);
    // Call KSU bridge logic
    return 0;
  }

  case DEEPEYE_PATCH_BOOT: {
    pr_info("IOCTL: Boot image patch request received\n");
    return 0;
  }

  default:
    return -ENOTTY;
  }
}

static struct file_operations fops = {
    .unlocked_ioctl = deepeye_ioctl,
    .owner = THIS_MODULE,
};

/**
 * deepeye_init - The entry point for the kernel module
 */
static int __init deepeye_init(void) {
  pr_info("DeepEyeUnlocker v4.0.0 Kernel Bridge Initialized\n");

  // 1. Register major number
  major_number = register_chrdev(0, DEVICE_NAME, &fops);
  if (major_number < 0) {
    pr_err("Failed to register a major number\n");
    return major_number;
  }

  // 2. Register device class
  deepeye_class = class_create(THIS_MODULE, CLASS_NAME);
  if (IS_ERR(deepeye_class)) {
    unregister_chrdev(major_number, DEVICE_NAME);
    pr_err("Failed to register device class\n");
    return PTR_ERR(deepeye_class);
  }

  // 3. Register device driver
  deepeye_device = device_create(deepeye_class, NULL, MKDEV(major_number, 0),
                                 NULL, DEVICE_NAME);
  if (IS_ERR(deepeye_device)) {
    class_destroy(deepeye_class);
    unregister_chrdev(major_number, DEVICE_NAME);
    pr_err("Failed to create the device\n");
    return PTR_ERR(deepeye_device);
  }

  deepeye_init_ksu();

  pr_info("Device created at /dev/%s\n", DEVICE_NAME);
  return 0;
}

/**
 * deepeye_exit - The cleanup point for the kernel module
 */
static void __exit deepeye_exit(void) {
  deepeye_exit_ksu();
  device_destroy(deepeye_class, MKDEV(major_number, 0));
  class_unregister(deepeye_class);
  class_destroy(deepeye_class);
  unregister_chrdev(major_number, DEVICE_NAME);
  pr_info("DeepEyeUnlocker Kernel Bridge Unloaded\n");
}

module_init(deepeye_init);
module_exit(deepeye_exit);
