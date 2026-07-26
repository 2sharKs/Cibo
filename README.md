# Cibo: High-Performance Logistics & Tracking Pipeline (PoC)

An architectural Proof of Concept (PoC) demonstrating a highly scalable, event-driven mobile client designed for hyper-local delivery tracking and real-time state synchronization.

## 🏗️ Architectural Overview (MVVM)
This mobile module is structured using clean Model-View-ViewModel (MVVM) separation of concerns to handle high-frequency telemetry data streams safely and efficiently:
- **Domain Layer (`DeliveryTelemetry`):** An immutable, type-safe data model driven by an explicit status Enum (`PLACED`, `PREPARING`, `OUT_FOR_DELIVERY`, `DELIVERED`).
- **Data Pipeline (`OrderTrackingRepository`):** Utilizes reactive **Kotlin Asynchronous Flows** forced onto `Dispatchers.IO` to ensure high-frequency location streams never execute on the main thread.
- **State Management (`OrderTrackingViewModel`):** Safely tracks incoming stream data frames within an explicit lifecycle-aware `viewModelScope` to expose an atomic UI state configuration.
- **Presentation Layer (`OrderTrackerScreen`):** A modern, responsive declarative UI layer written completely in **Jetpack Compose (Material3)**.

## 📈 Engineering Roadmap & System Milestones
- [x] **Phase 1 (Functional Native Client Core):** Asynchronous Flow state engine handling native thread-safe telemetry pipelines.
- [ ] **Phase 2 (Upcoming Multi-Threaded Core):** High-performance routing calculation model built natively in **C++** using graph optimization algorithms (A*/Dijkstra) hooked into the app via the Android NDK.
- [ ] **Phase 3 (Upcoming Edge AI Processing):** Integration of a local **Ollama** LLM adapter client to compute intelligent automated product substitutes during stockout events with zero API runtime costs.

## 🛠️ Local Development Setup
1. Clone this repository to your workstation.
2. Open the project directory inside **Android Studio (Ladybug or newer)**.
3. Sync the project via **Gradle**.
4. Deploy to an Android Virtual Device (Emulator running API level 33+) using the green **Run** button.
