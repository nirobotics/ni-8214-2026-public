'use strict';
const MANIFEST = 'flutter-app-manifest';
const TEMP = 'flutter-temp-cache';
const CACHE_NAME = 'flutter-app-cache';

const RESOURCES = {"flutter_bootstrap.js": "a0ad387a809862d0902d26dd07f78d0c",
"icons/Icon-512.png": "30cfc980b69e6aa785b521ed3241601d",
"icons/Icon-192.png": "69e7f92b1e2a028f186ae55e79c270ce",
"icons/Icon-maskable-512.png": "30cfc980b69e6aa785b521ed3241601d",
"icons/Icon-maskable-192.png": "69e7f92b1e2a028f186ae55e79c270ce",
"index.html": "66689e4aec74bd3ee763fb9dc933b597",
"/": "66689e4aec74bd3ee763fb9dc933b597",
"canvaskit/chromium/canvaskit.js": "a80c765aaa8af8645c9fb1aae53f9abf",
"canvaskit/chromium/canvaskit.wasm": "a726e3f75a84fcdf495a15817c63a35d",
"canvaskit/chromium/canvaskit.js.symbols": "e2d09f0e434bc118bf67dae526737d07",
"canvaskit/skwasm.js": "8060d46e9a4901ca9991edd3a26be4f0",
"canvaskit/skwasm_heavy.js.symbols": "0755b4fb399918388d71b59ad390b055",
"canvaskit/skwasm_heavy.wasm": "b0be7910760d205ea4e011458df6ee01",
"canvaskit/skwasm_heavy.js": "740d43a6b8240ef9e23eed8c48840da4",
"canvaskit/skwasm.js.symbols": "3a4aadf4e8141f284bd524976b1d6bdc",
"canvaskit/canvaskit.js": "8331fe38e66b3a898c4f37648aaf7ee2",
"canvaskit/canvaskit.wasm": "9b6a7830bf26959b200594729d73538e",
"canvaskit/skwasm.wasm": "7e5f3afdd3b0747a1fd4517cea239898",
"canvaskit/canvaskit.js.symbols": "a3c9f77715b642d0437d9c275caba91e",
"manifest.json": "2c97c7160cb02464d757b044a4c002b9",
"version.json": "b350b3ca48b6b30a32dcb4d1963a4bc3",
"main.dart.js": "9d72286d847cde9ab0fc7facc538bbbd",
"favicon.png": "cf2c108ce5b502be6eeb2d2eab8cad54",
"flutter.js": "24bc71911b75b5f8135c949e27a2984e",
"start_elastic.py": "8b304f1882b2e6f80c38691ef466a9a6",
"assets/AssetManifest.bin.json": "617fbd48989f5f26fb73dc84e65017b3",
"assets/AssetManifest.bin": "a281a5e531277a69403edd2ea33be76e",
"assets/FontManifest.json": "9f312a4847cd1accc80bbd07c1783b1c",
"assets/packages/titlebar_buttons/assets/themes/arc-dark/minimize-hover.svg": "5f9a3aeb2fda576cb33b3f569f471795",
"assets/packages/titlebar_buttons/assets/themes/arc-dark/minimize-active.svg": "2435c5211fa9ec6dfdd58560a1e10608",
"assets/packages/titlebar_buttons/assets/themes/arc-dark/close-hover.svg": "a76ebe248cfbd18b25e6b24db0c97d93",
"assets/packages/titlebar_buttons/assets/themes/arc-dark/maximize-active.svg": "7abea8d54af60af9c8539b76603aa6e5",
"assets/packages/titlebar_buttons/assets/themes/arc-dark/close.svg": "60df3479d5f36f99327c0e91d7f7f6a1",
"assets/packages/titlebar_buttons/assets/themes/arc-dark/maximize.svg": "731a7b0a424f120d7603072c91b97d7e",
"assets/packages/titlebar_buttons/assets/themes/arc-dark/minimize.svg": "f01942f9a5e0716d63826c0f6e60bb3c",
"assets/packages/titlebar_buttons/assets/themes/arc-dark/maximize-hover.svg": "56953703d7329d18388d8bc143fc787c",
"assets/packages/titlebar_buttons/assets/themes/arc-dark/close-active.svg": "e681b1b706c7858a06d0e7be83f5bffc",
"assets/packages/titlebar_buttons/assets/themes/pop-light/minimize-hover.svg": "487569a041eaff47a9f1a91c10176f5e",
"assets/packages/titlebar_buttons/assets/themes/pop-light/minimize-active.svg": "c57ce6e4efccd202303b6b1fe75e3a1e",
"assets/packages/titlebar_buttons/assets/themes/pop-light/close-hover.svg": "1f652b81f9d2ee6ce59701a3ffbacd88",
"assets/packages/titlebar_buttons/assets/themes/pop-light/maximize-active.svg": "fdb9e08b89fdff15ec9ea0d89d03be68",
"assets/packages/titlebar_buttons/assets/themes/pop-light/close.svg": "7a6960fa637cff15962284d87efc57e0",
"assets/packages/titlebar_buttons/assets/themes/pop-light/maximize.svg": "0836a19c0884e08f6ec9b7b794d83c66",
"assets/packages/titlebar_buttons/assets/themes/pop-light/minimize.svg": "0bd8e94b94f023b96f9bf35097e0e97d",
"assets/packages/titlebar_buttons/assets/themes/pop-light/maximize-hover.svg": "03a2b38b819bdbfb464ae0b76d815b48",
"assets/packages/titlebar_buttons/assets/themes/pop-light/close-active.svg": "c3df3141f42764e1f10d13f7d30e902f",
"assets/packages/titlebar_buttons/assets/themes/unity-light/minimize-hover.svg": "c1d705cf06c78e41ce5fcd5a1cbb9f84",
"assets/packages/titlebar_buttons/assets/themes/unity-light/minimize-active.svg": "10a2331f51c242c18db21f68d2811935",
"assets/packages/titlebar_buttons/assets/themes/unity-light/close-hover.svg": "124c659827868fe173f46231a0dd3532",
"assets/packages/titlebar_buttons/assets/themes/unity-light/maximize-active.svg": "4e318181d326ed3d46c2bd275d731822",
"assets/packages/titlebar_buttons/assets/themes/unity-light/close.svg": "8b839fe22d55828ba3f2662bda7bddb4",
"assets/packages/titlebar_buttons/assets/themes/unity-light/maximize.svg": "8f73064c5a5c0caadcd00de8aa1b35bb",
"assets/packages/titlebar_buttons/assets/themes/unity-light/minimize.svg": "dab67e9848c795621a8ce967c22160a9",
"assets/packages/titlebar_buttons/assets/themes/unity-light/maximize-hover.svg": "ac94ce4278ecba44d82e821f772441a7",
"assets/packages/titlebar_buttons/assets/themes/unity-light/close-active.svg": "d514da507d69229ff03fba9ba4c8fc84",
"assets/packages/titlebar_buttons/assets/themes/materia-light/minimize-hover.svg": "58b829cac511a81264297397f41669d6",
"assets/packages/titlebar_buttons/assets/themes/materia-light/minimize-active.svg": "7c9182f6e1166c320152ceb7090dd106",
"assets/packages/titlebar_buttons/assets/themes/materia-light/close-hover.svg": "9046ac30748b622fbbe6d43a4e3f1900",
"assets/packages/titlebar_buttons/assets/themes/materia-light/maximize-active.svg": "84deb05781da6d357b9ae7280d558d2a",
"assets/packages/titlebar_buttons/assets/themes/materia-light/close.svg": "51c5a252f944a641c41d2a6557236129",
"assets/packages/titlebar_buttons/assets/themes/materia-light/maximize.svg": "e7ee6348d44133aff1023d9f6d5d4f05",
"assets/packages/titlebar_buttons/assets/themes/materia-light/minimize.svg": "1adbd20e9f6ac10a2d5e52d4eddb4e4b",
"assets/packages/titlebar_buttons/assets/themes/materia-light/maximize-hover.svg": "606be773a7c7ac153b5e2eb2de665b84",
"assets/packages/titlebar_buttons/assets/themes/materia-light/close-active.svg": "a3c469f069dfc1358698dcbf9ef2adcb",
"assets/packages/titlebar_buttons/assets/themes/unity-dark/minimize-hover.svg": "5d6e49370bb6292a3a7e68108c2b6ccf",
"assets/packages/titlebar_buttons/assets/themes/unity-dark/minimize-active.svg": "7a670ccd56f5987d2e64f360c5b7cfad",
"assets/packages/titlebar_buttons/assets/themes/unity-dark/close-hover.svg": "124c659827868fe173f46231a0dd3532",
"assets/packages/titlebar_buttons/assets/themes/unity-dark/maximize-active.svg": "d1018320f58c656a54f7c62c31543e20",
"assets/packages/titlebar_buttons/assets/themes/unity-dark/close.svg": "8b839fe22d55828ba3f2662bda7bddb4",
"assets/packages/titlebar_buttons/assets/themes/unity-dark/maximize.svg": "10d075c84ca40947d8f3b29fc377b0da",
"assets/packages/titlebar_buttons/assets/themes/unity-dark/minimize.svg": "1882cecb367d85f1c1a1aefb72f32d9b",
"assets/packages/titlebar_buttons/assets/themes/unity-dark/maximize-hover.svg": "0d48c3018a70330467190156bd7d1858",
"assets/packages/titlebar_buttons/assets/themes/unity-dark/close-active.svg": "d514da507d69229ff03fba9ba4c8fc84",
"assets/packages/titlebar_buttons/assets/themes/arc-light/minimize-hover.svg": "e2d7d314918f368ce8b53ca1628aa342",
"assets/packages/titlebar_buttons/assets/themes/arc-light/minimize-active.svg": "2435c5211fa9ec6dfdd58560a1e10608",
"assets/packages/titlebar_buttons/assets/themes/arc-light/close-hover.svg": "5ec07421ed79dd216254ab0d079402a5",
"assets/packages/titlebar_buttons/assets/themes/arc-light/maximize-active.svg": "7abea8d54af60af9c8539b76603aa6e5",
"assets/packages/titlebar_buttons/assets/themes/arc-light/close.svg": "acf12ad5a7681226e8886bfb820c4fd9",
"assets/packages/titlebar_buttons/assets/themes/arc-light/maximize.svg": "2af431cea3540e99332393f6db520d4b",
"assets/packages/titlebar_buttons/assets/themes/arc-light/minimize.svg": "d199f48a392a12726f038698ba3f8c0f",
"assets/packages/titlebar_buttons/assets/themes/arc-light/maximize-hover.svg": "8662d3562d78888c07e0c87d2e0a7716",
"assets/packages/titlebar_buttons/assets/themes/arc-light/close-active.svg": "2e1bbc4f106655b6fb8324329bdb4454",
"assets/packages/titlebar_buttons/assets/themes/breeze/minimize-hover.svg": "7f5bb55e1fb0f9a71b1df5cee14a51aa",
"assets/packages/titlebar_buttons/assets/themes/breeze/minimize-active.svg": "c1da68a49406dee102800216459e96a7",
"assets/packages/titlebar_buttons/assets/themes/breeze/close-hover.svg": "2bea2ace565b419b72b3e6120db3cd21",
"assets/packages/titlebar_buttons/assets/themes/breeze/maximize-active.svg": "150ef2dbac77270f6f7dda2503edadfe",
"assets/packages/titlebar_buttons/assets/themes/breeze/close.svg": "336b4edede351a886522ad9fa341a338",
"assets/packages/titlebar_buttons/assets/themes/breeze/maximize.svg": "38ea0310e127a1b0800c0cdb5c163f77",
"assets/packages/titlebar_buttons/assets/themes/breeze/minimize.svg": "18366daa6cccaf370daf49311c01655b",
"assets/packages/titlebar_buttons/assets/themes/breeze/maximize-hover.svg": "52a77c2d238fc4d826b6d0c114999fcb",
"assets/packages/titlebar_buttons/assets/themes/breeze/close-active.svg": "3200eab6937fab0055ee9b7e9a5587bb",
"assets/packages/titlebar_buttons/assets/themes/yaru/minimize-hover.svg": "e86f9232cfc405852e1cc35474475c8e",
"assets/packages/titlebar_buttons/assets/themes/yaru/minimize-active.svg": "66a4aeedd2717f10e3eb75705ab356c5",
"assets/packages/titlebar_buttons/assets/themes/yaru/close-hover.svg": "d2041c2890f5f85f694a521db7ea6e6e",
"assets/packages/titlebar_buttons/assets/themes/yaru/maximize-active.svg": "9b3c8b210dcff3ca65b5aaf277ae2cf1",
"assets/packages/titlebar_buttons/assets/themes/yaru/close.svg": "8ff63df0a22e9b7dd536a0d5d0c83925",
"assets/packages/titlebar_buttons/assets/themes/yaru/maximize.svg": "dac8806e404397bfe3f45fbf245bc62d",
"assets/packages/titlebar_buttons/assets/themes/yaru/minimize.svg": "eb1190735d11f3bb79932c81406bd8d4",
"assets/packages/titlebar_buttons/assets/themes/yaru/maximize-hover.svg": "a466339be81e009a4a75f0e1f3c15b53",
"assets/packages/titlebar_buttons/assets/themes/yaru/close-active.svg": "cf8f7ec63162a2a35b44523d738aedff",
"assets/packages/titlebar_buttons/assets/themes/osx-arc/minimize-hover.svg": "f78c2ba47d388d2dde5a9bdb0d46817c",
"assets/packages/titlebar_buttons/assets/themes/osx-arc/minimize-active.svg": "f78c2ba47d388d2dde5a9bdb0d46817c",
"assets/packages/titlebar_buttons/assets/themes/osx-arc/close-hover.svg": "82d218a05559635ce45bcec48897e799",
"assets/packages/titlebar_buttons/assets/themes/osx-arc/maximize-active.svg": "b2db20bb0bcc84fde9fe3274c1e07e7a",
"assets/packages/titlebar_buttons/assets/themes/osx-arc/close.svg": "2fe2d234adbc2b42e07dbe61670eeca6",
"assets/packages/titlebar_buttons/assets/themes/osx-arc/maximize.svg": "0015ddb1a8e73c53ee476ce2e9c7c267",
"assets/packages/titlebar_buttons/assets/themes/osx-arc/minimize.svg": "e0b223a27980f3d44b3c988da551ccfc",
"assets/packages/titlebar_buttons/assets/themes/osx-arc/maximize-hover.svg": "b2db20bb0bcc84fde9fe3274c1e07e7a",
"assets/packages/titlebar_buttons/assets/themes/osx-arc/close-active.svg": "82d218a05559635ce45bcec48897e799",
"assets/packages/titlebar_buttons/assets/themes/pop-dark/minimize-hover.svg": "771267bf43da2dc6f7b4a6a07345e1b1",
"assets/packages/titlebar_buttons/assets/themes/pop-dark/minimize-active.svg": "26567950c62adb2db1199fb3e96faf8d",
"assets/packages/titlebar_buttons/assets/themes/pop-dark/close-hover.svg": "1f652b81f9d2ee6ce59701a3ffbacd88",
"assets/packages/titlebar_buttons/assets/themes/pop-dark/maximize-active.svg": "7c7509958c47b71ce5fc3a02ec492154",
"assets/packages/titlebar_buttons/assets/themes/pop-dark/close.svg": "7a6960fa637cff15962284d87efc57e0",
"assets/packages/titlebar_buttons/assets/themes/pop-dark/maximize.svg": "fcec63844e30fa683574c06f333385c6",
"assets/packages/titlebar_buttons/assets/themes/pop-dark/minimize.svg": "100f400ffab7ba2dc422a0031ddcfd27",
"assets/packages/titlebar_buttons/assets/themes/pop-dark/maximize-hover.svg": "4b8e501a0151db9aa10edf4978c213af",
"assets/packages/titlebar_buttons/assets/themes/pop-dark/close-active.svg": "c3df3141f42764e1f10d13f7d30e902f",
"assets/packages/titlebar_buttons/assets/themes/vimix/minimize-hover.svg": "76a7ce5f21af44addd142610373beed5",
"assets/packages/titlebar_buttons/assets/themes/vimix/minimize-active.svg": "1d9571dab6cc38bc64336a2162921442",
"assets/packages/titlebar_buttons/assets/themes/vimix/close-hover.svg": "e696ec5a29731115a226099d6cfccc4f",
"assets/packages/titlebar_buttons/assets/themes/vimix/maximize-active.svg": "fba2a6ae55febaf1c35ab3556aa207d8",
"assets/packages/titlebar_buttons/assets/themes/vimix/close.svg": "85bf50fb57d965880d0e945845102831",
"assets/packages/titlebar_buttons/assets/themes/vimix/maximize.svg": "804d68402ac8659f97996e0c9494e68b",
"assets/packages/titlebar_buttons/assets/themes/vimix/minimize.svg": "af14426de6839b83c640f872eda23b52",
"assets/packages/titlebar_buttons/assets/themes/vimix/maximize-hover.svg": "de8913dcd654f8661699c3a2da307b0a",
"assets/packages/titlebar_buttons/assets/themes/vimix/close-active.svg": "8638ec609362fdd99ca5ab0d11bf37de",
"assets/packages/titlebar_buttons/assets/themes/elementary/close.svg": "738da940ef6d978aab28afa3f590a721",
"assets/packages/titlebar_buttons/assets/themes/elementary/maximize.svg": "3eca4448fa1c753eee82a0a20e7ea89c",
"assets/packages/titlebar_buttons/assets/themes/elementary/minimize.svg": "1cfa1dc42a3421ef6649e49c8af5d006",
"assets/packages/titlebar_buttons/assets/themes/adwaita/minimize-hover.svg": "611539ab0eaae0a05309cadd087d9fb6",
"assets/packages/titlebar_buttons/assets/themes/adwaita/minimize-active.svg": "5148b7558428d30741fb2102b61d0090",
"assets/packages/titlebar_buttons/assets/themes/adwaita/close-hover.svg": "9a7b6ca37751496dd4faa6b5c6e44344",
"assets/packages/titlebar_buttons/assets/themes/adwaita/maximize-active.svg": "fdaf450b8fce1de545e91fcdaa54f4e3",
"assets/packages/titlebar_buttons/assets/themes/adwaita/close.svg": "495858da9474f76e1e55eac0813e96c2",
"assets/packages/titlebar_buttons/assets/themes/adwaita/maximize.svg": "eca509988316e7020433935a4f748821",
"assets/packages/titlebar_buttons/assets/themes/adwaita/minimize.svg": "e434e115af18b857498b404e70c0c588",
"assets/packages/titlebar_buttons/assets/themes/adwaita/maximize-hover.svg": "153a3b771f482ca852fc5db94e272c53",
"assets/packages/titlebar_buttons/assets/themes/adwaita/close-active.svg": "3a073e2349af7701b12708a74a685f82",
"assets/packages/titlebar_buttons/assets/themes/flat-remix/minimize-hover.svg": "f0fd64817e4534eb5c77429d474563d2",
"assets/packages/titlebar_buttons/assets/themes/flat-remix/minimize-active.svg": "f0fd64817e4534eb5c77429d474563d2",
"assets/packages/titlebar_buttons/assets/themes/flat-remix/close-hover.svg": "0d4339008871c8486e7fec0edf4f557b",
"assets/packages/titlebar_buttons/assets/themes/flat-remix/maximize-active.svg": "bd9e3dfe1fd4bfdfff8283f9994f4754",
"assets/packages/titlebar_buttons/assets/themes/flat-remix/close.svg": "c43a377d111df1e1d2f37a231898865e",
"assets/packages/titlebar_buttons/assets/themes/flat-remix/maximize.svg": "b2281965f7e04c8c7cb340d8345e0fe2",
"assets/packages/titlebar_buttons/assets/themes/flat-remix/minimize.svg": "e5955a935f9b37739530e523ae5cc281",
"assets/packages/titlebar_buttons/assets/themes/flat-remix/maximize-hover.svg": "bd9e3dfe1fd4bfdfff8283f9994f4754",
"assets/packages/titlebar_buttons/assets/themes/flat-remix/close-active.svg": "0d4339008871c8486e7fec0edf4f557b",
"assets/packages/titlebar_buttons/assets/themes/nordic/minimize-hover.svg": "1a2d3d6635a522bb3a68c21ece907b41",
"assets/packages/titlebar_buttons/assets/themes/nordic/minimize-active.svg": "b50eaba2d557c5d11c41a315c496f1dd",
"assets/packages/titlebar_buttons/assets/themes/nordic/close-hover.svg": "e379147e3b002b65b52f065f5475f018",
"assets/packages/titlebar_buttons/assets/themes/nordic/maximize-active.svg": "c4ef0521818dbc41ae019db685b27fb4",
"assets/packages/titlebar_buttons/assets/themes/nordic/close.svg": "100d0f49953721d16bd9b30bbbdc0706",
"assets/packages/titlebar_buttons/assets/themes/nordic/maximize.svg": "08f662d88e4fd59a540285d78387e087",
"assets/packages/titlebar_buttons/assets/themes/nordic/minimize.svg": "6d3a5c751e085386a932c4f41a0bdefd",
"assets/packages/titlebar_buttons/assets/themes/nordic/maximize-hover.svg": "69f52365b38cedcf0cbd559599ee4309",
"assets/packages/titlebar_buttons/assets/themes/nordic/close-active.svg": "fa0dbc9f70651980a723ded6a51e3d89",
"assets/packages/titlebar_buttons/assets/themes/materia-dark/minimize-hover.svg": "7c44633fd1dde56acfc0004cfe6eecee",
"assets/packages/titlebar_buttons/assets/themes/materia-dark/minimize-active.svg": "7fcdfaf115b2f31cf16f4bf0193b72fe",
"assets/packages/titlebar_buttons/assets/themes/materia-dark/close-hover.svg": "6e805c26d527b9e5fa28e0936e4b4fb6",
"assets/packages/titlebar_buttons/assets/themes/materia-dark/maximize-active.svg": "fc327ee664e9f5661d1131b53563e6a6",
"assets/packages/titlebar_buttons/assets/themes/materia-dark/close.svg": "85d9057dc05198bbc605ae41339d3f4b",
"assets/packages/titlebar_buttons/assets/themes/materia-dark/maximize.svg": "663f0a4137258f2c40e64667a7b0d1ea",
"assets/packages/titlebar_buttons/assets/themes/materia-dark/minimize.svg": "d47bd0e7a3c25e6baf902e1ed58b6066",
"assets/packages/titlebar_buttons/assets/themes/materia-dark/maximize-hover.svg": "6400b61acc2790ad864726b9c2ae2949",
"assets/packages/titlebar_buttons/assets/themes/materia-dark/close-active.svg": "9fb4fe9bffc7f4e34e1fdecdd146b04f",
"assets/packages/geekyants_flutter_gauges/assets/fonts/Roboto-Regular.ttf": "8a36205bd9b83e03af0591a004bc97f4",
"assets/fonts/MaterialIcons-Regular.otf": "a1c99f707f684672593a96c08310762d",
"assets/NOTICES": "7eb64d549cf18017580a18cf71114a2f",
"assets/shaders/stretch_effect.frag": "40d68efbbf360632f614c731219e95f0",
"assets/shaders/ink_sparkle.frag": "ecc85a2e95f5e9f53123dcaf8cb9b6ce",
"assets/assets/fields/2020-field.png": "57d11cbffba225bd2b0f59b930cdd426",
"assets/assets/fields/2026-rebuilt-no-fuel.json": "48f7e0ace5a86684244a1e967b57103b",
"assets/assets/fields/2026-field-no-fuel.png": "574c7aac98312a7ca242dc6f155c925d",
"assets/assets/fields/2019-deepspace.json": "fbb784eaeac80c0cff6b18933c1ed08a",
"assets/assets/fields/2025-reefscape.json": "9ebb6a3c601045e6ad76db50de27cbee",
"assets/assets/fields/2024-field.png": "bdd134556e50cf3dd28bc71590f66301",
"assets/assets/fields/2024-crescendo.json": "0cf28d235fee24c0f8059e2119f1b3ec",
"assets/assets/fields/2022-field.png": "0c3798981f06bdedfa2cb617bf2e97b6",
"assets/assets/fields/2023-field.png": "497d479865bab2b669061f57ab5c8216",
"assets/assets/fields/2018-powerup.json": "fdc4b9fb1ab8c714572ec830a6e62e4b",
"assets/assets/fields/2022-rapidreact.json": "40cd771a81b0202852a7d5dfb12139af",
"assets/assets/fields/2025-field.png": "462c3a733899f51fb8f620db0e2083ed",
"assets/assets/fields/2019-field.png": "ba9a873580f15785df6b26244b1c3ba2",
"assets/assets/fields/2026-rebuilt.json": "5586b2c2335381fd416d3cae0176c2d7",
"assets/assets/fields/2023-chargedup.json": "178aa0c27e6598c431347913c0c99171",
"assets/assets/fields/2018-field.png": "61f2d778509e944e6ab090015bed4b1f",
"assets/assets/fields/2026-field.png": "d084b25de6f952a447c00e13bf8ea01d",
"assets/assets/fields/2020-infiniterecharge.json": "90e69a926dab7c0ef63f187403b1d15d",
"assets/assets/logos/wpilib_logo.png": "a78000ac0477fb83e99d760a5b492c2c",
"assets/assets/logos/logo_full.png": "012a5979c4eec4389586fa29d6258cf3",
"assets/assets/logos/logo.png": "bf5c0afb35675bdaacf7df27cd3e11c6",
"assets/assets/fonts/Roboto-Regular.ttf": "303c6d9e16168364d3bc5b7f766cfff4",
"assets/assets/fonts/Roboto-Bold.ttf": "8c9110ec6a1737b15a5611dc810b0f92",
"assets/assets/fonts/Roboto-Medium.ttf": "7d752fb726f5ece291e2e522fcecf86d",
"assets/assets/third_party_licenses/AdvantageScopeAssets.txt": "d0aa3ce04744b208a95771b2ba7d50d2",
"assets/assets/third_party_licenses/OFL.txt": "d0d30e4860cc10440e095be065c2b0e3"};
// The application shell files that are downloaded before a service worker can
// start.
const CORE = ["main.dart.js",
"index.html",
"flutter_bootstrap.js",
"assets/AssetManifest.bin.json",
"assets/FontManifest.json"];

// During install, the TEMP cache is populated with the application shell files.
self.addEventListener("install", (event) => {
  self.skipWaiting();
  return event.waitUntil(
    caches.open(TEMP).then((cache) => {
      return cache.addAll(
        CORE.map((value) => new Request(value, {'cache': 'reload'})));
    })
  );
});
// During activate, the cache is populated with the temp files downloaded in
// install. If this service worker is upgrading from one with a saved
// MANIFEST, then use this to retain unchanged resource files.
self.addEventListener("activate", function(event) {
  return event.waitUntil(async function() {
    try {
      var contentCache = await caches.open(CACHE_NAME);
      var tempCache = await caches.open(TEMP);
      var manifestCache = await caches.open(MANIFEST);
      var manifest = await manifestCache.match('manifest');
      // When there is no prior manifest, clear the entire cache.
      if (!manifest) {
        await caches.delete(CACHE_NAME);
        contentCache = await caches.open(CACHE_NAME);
        for (var request of await tempCache.keys()) {
          var response = await tempCache.match(request);
          await contentCache.put(request, response);
        }
        await caches.delete(TEMP);
        // Save the manifest to make future upgrades efficient.
        await manifestCache.put('manifest', new Response(JSON.stringify(RESOURCES)));
        // Claim client to enable caching on first launch
        self.clients.claim();
        return;
      }
      var oldManifest = await manifest.json();
      var origin = self.location.origin;
      for (var request of await contentCache.keys()) {
        var key = request.url.substring(origin.length + 1);
        if (key == "") {
          key = "/";
        }
        // If a resource from the old manifest is not in the new cache, or if
        // the MD5 sum has changed, delete it. Otherwise the resource is left
        // in the cache and can be reused by the new service worker.
        if (!RESOURCES[key] || RESOURCES[key] != oldManifest[key]) {
          await contentCache.delete(request);
        }
      }
      // Populate the cache with the app shell TEMP files, potentially overwriting
      // cache files preserved above.
      for (var request of await tempCache.keys()) {
        var response = await tempCache.match(request);
        await contentCache.put(request, response);
      }
      await caches.delete(TEMP);
      // Save the manifest to make future upgrades efficient.
      await manifestCache.put('manifest', new Response(JSON.stringify(RESOURCES)));
      // Claim client to enable caching on first launch
      self.clients.claim();
      return;
    } catch (err) {
      // On an unhandled exception the state of the cache cannot be guaranteed.
      console.error('Failed to upgrade service worker: ' + err);
      await caches.delete(CACHE_NAME);
      await caches.delete(TEMP);
      await caches.delete(MANIFEST);
    }
  }());
});
// The fetch handler redirects requests for RESOURCE files to the service
// worker cache.
self.addEventListener("fetch", (event) => {
  if (event.request.method !== 'GET') {
    return;
  }
  var origin = self.location.origin;
  var key = event.request.url.substring(origin.length + 1);
  // Redirect URLs to the index.html
  if (key.indexOf('?v=') != -1) {
    key = key.split('?v=')[0];
  }
  if (event.request.url == origin || event.request.url.startsWith(origin + '/#') || key == '') {
    key = '/';
  }
  // If the URL is not the RESOURCE list then return to signal that the
  // browser should take over.
  if (!RESOURCES[key]) {
    return;
  }
  // If the URL is the index.html, perform an online-first request.
  if (key == '/') {
    return onlineFirst(event);
  }
  event.respondWith(caches.open(CACHE_NAME)
    .then((cache) =>  {
      return cache.match(event.request).then((response) => {
        // Either respond with the cached resource, or perform a fetch and
        // lazily populate the cache only if the resource was successfully fetched.
        return response || fetch(event.request).then((response) => {
          if (response && Boolean(response.ok)) {
            cache.put(event.request, response.clone());
          }
          return response;
        });
      })
    })
  );
});
self.addEventListener('message', (event) => {
  // SkipWaiting can be used to immediately activate a waiting service worker.
  // This will also require a page refresh triggered by the main worker.
  if (event.data === 'skipWaiting') {
    self.skipWaiting();
    return;
  }
  if (event.data === 'downloadOffline') {
    downloadOffline();
    return;
  }
});
// Download offline will check the RESOURCES for all files not in the cache
// and populate them.
async function downloadOffline() {
  var resources = [];
  var contentCache = await caches.open(CACHE_NAME);
  var currentContent = {};
  for (var request of await contentCache.keys()) {
    var key = request.url.substring(origin.length + 1);
    if (key == "") {
      key = "/";
    }
    currentContent[key] = true;
  }
  for (var resourceKey of Object.keys(RESOURCES)) {
    if (!currentContent[resourceKey]) {
      resources.push(resourceKey);
    }
  }
  return contentCache.addAll(resources);
}
// Attempt to download the resource online before falling back to
// the offline cache.
function onlineFirst(event) {
  return event.respondWith(
    fetch(event.request).then((response) => {
      return caches.open(CACHE_NAME).then((cache) => {
        cache.put(event.request, response.clone());
        return response;
      });
    }).catch((error) => {
      return caches.open(CACHE_NAME).then((cache) => {
        return cache.match(event.request).then((response) => {
          if (response != null) {
            return response;
          }
          throw error;
        });
      });
    })
  );
}
