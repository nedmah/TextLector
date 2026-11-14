import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    init() {
        let repo = IosVoiceModelRepositoryImpl()
        let sherpaEngine = IosSherpaEngine(repository: repo)
        let supertonicRepository = IosSupertonicRepository()
        let supertonicEngine = IosSupertonicEngine(repository: supertonicRepository)

        IosEngineHolder.shared.sherpaEngine = sherpaEngine
        IosEngineHolder.shared.supertonicEngine = supertonicEngine
        IosEngineHolder.shared.supertonicRepository = supertonicRepository

        IosEngineHolder.shared.tarExtractor = IosTarExtractor()
        IosEngineHolder.shared.ocrEngine = IosOcrEngine()
        IosEngineHolder.shared.fileDownloader = IosFileDownloader()
        IosEngineHolder.shared.pdfExtractor = IosPdfPageExtractor()
        MainViewControllerKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
