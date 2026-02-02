#ifndef DEEPEYE_KERNEL_H
#define DEEPEYE_KERNEL_H

#include <linux/ioctl.h>
#include <linux/types.h>

/* IOCTL Definition */
#define DEEPEYE_MAGIC 'D'

/* Argument structures */
struct hide_root_args {
  int pid;
  char package_name[256];
  int flags;
};

struct boot_patch_args {
  uint64_t image_ptr;
  size_t size;
  int patch_type;
};

/* IOCTL Commands */
#define DEEPEYE_HIDE_ROOT _IOW(DEEPEYE_MAGIC, 1, struct hide_root_args)
#define DEEPEYE_UNHIDE_ROOT _IOW(DEEPEYE_MAGIC, 2, struct hide_root_args)
#define DEEPEYE_PATCH_BOOT _IOW(DEEPEYE_MAGIC, 3, struct boot_patch_args)

/* Feature Prototypes */
int deepeye_init_ksu(void);
void deepeye_exit_ksu(void);

#endif /* DEEPEYE_KERNEL_H */
