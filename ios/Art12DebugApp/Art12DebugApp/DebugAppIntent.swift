import AppIntents

struct RunDebugActionIntent: AppIntent {
    static var title: LocalizedStringResource = "Run Debug Action"
    static var description = IntentDescription("Returns a quick debug confirmation from the Art12 app.")

    func perform() async throws -> some IntentResult & ProvidesDialog {
        .result(dialog: "Art12 debug action completed.")
    }
}

struct Art12DebugShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: RunDebugActionIntent(),
            phrases: [
                "Run debug action in \(.applicationName)",
                "Start Art12 debug in \(.applicationName)"
            ],
            shortTitle: "Debug Action",
            systemImageName: "hammer.circle"
        )
    }
}
