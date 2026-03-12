//
//  ContentView.swift
//  treeNav
//
//  Created by adetunji_dahunsi on 3/1/26.
//

import common
import SwiftUI

struct ContentView: View {
    var body: some View {
        VStack {
            Image(systemName: "globe")
                .imageScale(.large)
                .foregroundStyle(.tint)
            Text("Hello, world!")
            ComposeView()
                // Compose has own keyboard handler
                .ignoresSafeArea(edges: .bottom)
        }
        .padding()
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let controller = Main_iosKt.MainViewController()
        controller.overrideUserInterfaceStyle = .light
        return controller
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

#Preview {
    ContentView()
}
