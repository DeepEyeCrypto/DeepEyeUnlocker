#include "../include/boot_image.h"
#include <pybind11/pybind11.h>
#include <pybind11/stl.h>

namespace py = pybind11;

PYBIND11_MODULE(deepeye_kernel, m) {
  m.doc() = "DeepEyeUnlocker Kernel Tool Python Bindings";

  py::class_<deepeye::BootImage>(m, "BootImage")
      .def(py::init<>())
      .def_readwrite("version", &deepeye::BootImage::version)
      .def_readwrite("cmdline", &deepeye::BootImage::cmdline)
      .def("load", &deepeye::BootImage::load, "Load boot image from path")
      .def("save", &deepeye::BootImage::save, "Save boot image to path")
      .def_property(
          "kernel",
          [](deepeye::BootImage &self) {
            return py::bytes((char *)self.kernel.data(), self.kernel.size());
          },
          [](deepeye::BootImage &self, py::bytes b) {
            std::string s = b;
            self.kernel.assign(s.begin(), s.end());
          })
      .def_property(
          "ramdisk",
          [](deepeye::BootImage &self) {
            return py::bytes((char *)self.ramdisk.data(), self.ramdisk.size());
          },
          [](deepeye::BootImage &self, py::bytes b) {
            std::string s = b;
            self.ramdisk.assign(s.begin(), s.end());
          });
}
