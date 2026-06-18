import SwiftUI

struct ContentView: View {
    @State private var tapCount = 0
    @State private var status = "Ready"

    var body: some View {
        NavigationStack {
            VStack(spacing: 18) {
                Image(systemName: "hammer.circle.fill")
                    .font(.system(size: 68))
                    .foregroundStyle(.teal)
                    .accessibilityHidden(true)

                VStack(spacing: 8) {
                    Text("Art12 Debug App")
                        .font(.largeTitle.bold())

                    Text(status)
                        .font(.headline)
                        .foregroundStyle(.secondary)
                        .accessibilityIdentifier("statusText")
                }

                Button {
                    tapCount += 1
                    status = "Tapped \(tapCount) time\(tapCount == 1 ? "" : "s")"
                } label: {
                    Label("Run Debug Action", systemImage: "play.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .accessibilityIdentifier("debugActionButton")

                Text("Use this screen for simulator smoke tests, breakpoints, and UI automation.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding(24)
            .navigationTitle("Debug")
        }
    }
}

#Preview {
    ContentView()
}
