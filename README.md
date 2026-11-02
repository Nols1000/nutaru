# nutaru

*nutaru* is a free, privacy-focused calorie tracking app for iPhone, Android and 
web at [nutaru.com](https://nutaru.com). Log food, weight and health data, see trends over time, 
and work toward your goals. No accounts, subscriptions, ads, or monetization.

<p>
  <a href="">
    <img src="./docs/assets/Download_on_the_App_Store_Badge.svg" alt="Download on the App Store" height="48px" />
  </a>
  <a href="">
    <img src="./docs/assets/Get_It_On_Google_Play_Badge.svg" alt="Get it on Google Play" height="48px" />
  </a>
</p>

<p>
  <img src="https://placehold.co/1320x2868.png" width="200px"  alt="Screenshot placeholder 1"/>
  <img src="https://placehold.co/1320x2868.png" width="200px"  alt="Screenshot placeholder 2"/>
  <img src="https://placehold.co/1320x2868.png" width="200px"  alt="Screenshot placeholder 3"/>
  <img src="https://placehold.co/1320x2868.png" width="200px"  alt="Screenshot placeholder 4"/>
  <img src="https://placehold.co/1320x2868.png" width="200px"  alt="Screenshot placeholder 5"/>
</p>

## Roadmap

- [ ] Record plates 
  - [ ] by manually entered calories and other nutrition information
  - [ ] by searching products in the local food product catalogue
  - [ ] by searching product barcodes in the local food product catalogue
  - [ ] by manually creating a new product
  - [ ] by guessing food products and amounts with an on-device AI model
  - [ ] by searching recipes in the local recipes catalogue
- [ ] Record body metrics
  - [ ] age
  - [ ] height
  - [ ] weight
  - [ ] fat free mass
- [ ] Set goals
  - [ ] daily calories
  - [ ] daily macro-nutrients &sup1;
  - [ ] weight
  - [ ] fat free mass
  - [ ] calculate calories goal based on weight goals and adapt periodically (weekly, monthly)
- [ ] Explore insights
  - [ ] calories
  - [ ] nutrients 
    - [ ] macro-nutrients &sup1;
    - [ ] micro-nutrients &sup2;
  - [ ] weight
  - [ ] fat free mass
  - [ ] total daily energy expenditure (estimated)
  - [ ] standard timeframes (1d, 7d, 30d, 90d, 180d)
  - [ ] custom timeframes
  - [ ] transparent missing nutrient information (5/104 products miss nutrient x information)
    - [ ] detail view of the products with missing information
- [ ] Create recipes
- [ ] Import nutrition information from open data projects such as open food facts
- [ ] Release on Google Play, App Store and [nutaru.com](https://nutaru.com)

<details>
    <summary>&sup1; macro-nutrients</summary>

- [ ] proteins
- [ ] fats
- [ ] carbohydrates
</details>

<details>
    <summary>&sup2; micro-nutrients</summary>
  
- [ ] proteins
    - [ ] cysteine
    - [ ] histidine
    - [ ] isoleucine
    - [ ] leucine
    - [ ] lysine
    - [ ] methionine
    - [ ] phenylalanine
    - [ ] threonine
    - [ ] tryptophan
    - [ ] tyrosine
    - [ ] valine
- [ ] fats
    - [ ] monounsaturated
    - [ ] polyunsaturated
    - [ ] omega-3
        - [ ] ALA
        - [ ] DHA
        - [ ] EPA
    - [ ] omega-6
    - [ ] saturated
    - [ ] trans fats
- [ ] cards
    - [ ] fibers
    - [ ] sugars
    - [ ] added sugars
- [ ] vitamins
    - [ ] b1, thiamine
    - [ ] b2, riboflavin
    - [ ] b3, niacin
    - [ ] b5, pantothenic acid
    - [ ] b6, pyridoxine
    - [ ] b12, cobalamin
    - [ ] folate
    - [ ] a
    - [ ] c
    - [ ] d
    - [ ] e
    - [ ] k
- [ ] minerals
    - [ ] calcium
    - [ ] copper
    - [ ] iron
    - [ ] magnesium
    - [ ] manganese
    - [ ] phosphorus
    - [ ] potassium
    - [ ] selenium
    - [ ] sodium
    - [ ] zinc
- [ ] others
    - [ ] alcohol
    - [ ] caffeine
    - [ ] cholesterol
    - [ ] choline
    - [ ] water
</details>

---

# For Contributors / Developers

## Getting Started

### Prerequisites

- [Git](https://git-scm.com/) - Manages source code version
- [JDK 21](https://adoptium.net/) - Runs Gradle; the Gradle daemon JVM is pinned to Java 21
- [Android Studio](https://developer.android.com/studio) - Provides the Android SDK and emulator used to build and run the android app

<details>
  <summary>iOS</summary>

  - [Xcode](https://developer.apple.com/xcode/) - Only needed for the iOS app; building the iOS app requires macOS and Xcode
</details>

<details>
  <summary>Web</summary>

  - [Node.js](https://nodejs.org/) - Only needed for the web app; any LTS version works
</details>

### Building

```shell
./gradlew :app:androidApp:assembleDebug
```
<details>
  <summary>iOS</summary>

  ```shell
  xcodebuild -project app/iosApp/iosApp.xcodeproj -scheme iosApp \
    -configuration Debug -destination 'generic/platform=iOS Simulator' build
  ```
</details>

<details>
  <summary>Web</summary>

  For modern browsers:
  ```shell
  ./gradlew :app:webApp:wasmJsBrowserDevelopmentRun
  ```

  Only for older browsers without WASM support:
  ```shell
  ./gradlew :app:webApp:jsBrowserDevelopmentRun
  ```
</details>


## Testing

```sh
./gradlew :app:sharedLogic:testAndroidHostTest
```

<details>
  <summary>iOS</summary>

  ```shell
  ./gradlew :app:sharedLogic:iosSimulatorArm64Test
  ```
</details>

<details>
  <summary>Web</summary>

  For modern browsers:
  ```shell
  ./gradlew :app:sharedLogic:wasmJsTest
  ```

  Only for older browsers without WASM support:
  ```shell
  ./gradlew :app:sharedLogic:jsTest
  ```
</details>

## Built With

- [Kotlin](https://kotlinlang.org/) - Language used for all shared code and UI
- [TypeScript](https://www.typescriptlang.org/) - Web app code, including the generated definitions of the shared Kotlin library
- [KMP](https://kotlinlang.org/docs/multiplatform.html) - Compiles the shared modules for android, iOS, desktop and web
- [Swift UI](https://developer.apple.com/xcode/swiftui/) - iOS app embedding the shared frameworks
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) - Declarative UI framework of the android and desktop apps
- [React](https://react.dev/) - Web app consuming the shared Kotlin/JS library
- [Vite](https://vite.dev/) - Dev server and bundler of the web appy

## Contributing

Please read [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for details on our code of conduct, and 
[CONTRIBUTING.md](CONTRIBUTING.md) the process for submitting pull requests to us.

## Versioning

We use [Semantic Versioning](http://semver.org/) for versioning. For the versions available, see the 
[tags on this repository](https://github.com/Nols1000/nutaru/tags).

## Authors

- **Nils-Börge Margotti** - *Owner* - [Nols1000](https://github.com/Nols1000)

See also the list of [contributors](https://github.com/Nols1000/nutaru/contributors) who 
participated in this project.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Hat tip to anyone whose code is used
- Inspiration
- etc
