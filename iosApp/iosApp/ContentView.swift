import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .onReceive(NotificationCenter.default.publisher(for: .init("ForceOrientationLandscapeRight"))) { _ in
                forceLandscapeOrientation()
            }
    }
}

private func forceLandscapeOrientation() {
    DispatchQueue.main.async {
        UIDevice.current.setValue(UIDeviceOrientation.landscapeRight.rawValue, forKey: "orientation")
    }
}



