import AppIntents

struct RunDebugActionIntent: AppIntent {
    static var title: LocalizedStringResource = "Run Smoke Test"
    static var description = IntentDescription("Returns a quick smoke-test confirmation from the 艺本艺术 iOS shell app.")

    func perform() async throws -> some IntentResult & ProvidesDialog {
        .result(dialog: "艺本艺术 iOS smoke test completed.")
    }
}

struct Art12DebugShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: RunDebugActionIntent(),
            phrases: [
                "Run smoke test in \(.applicationName)",
                "Start 艺本艺术 test build in \(.applicationName)"
            ],
            shortTitle: "Smoke Test",
            systemImageName: "hammer.circle"
        )
    }
}
