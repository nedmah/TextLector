import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    init() {
        let repo = IosVoiceModelRepositoryImpl()
        let sherpaEngine = IosSherpaEngine(repository: repo)
        IosEngineHolder.shared.ttsEngine = sherpaEngine
        IosEngineHolder.shared.tarExtractor = IosTarExtractor()
        IosEngineHolder.shared.fileDownloader = IosFileDownloader()
        MainViewControllerKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
