// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name:     "DeepEyeCore",
    platforms: [.macOS(.v12)],
    products: [
        .executable(name: "deepeye-core", targets: ["DeepEyeCore"]),
    ],
    dependencies: [],   // zero external deps — pure Foundation
    targets: [
        .executableTarget(
            name:       "DeepEyeCore",
            path:       "Sources/DeepEyeCore",
            swiftSettings: [
                .unsafeFlags(["-O", "-whole-module-optimization"]),
            ]
        ),
    ]
)
