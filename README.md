# FrameChecky

**Ever downloaded a game just to find out your phone turns into a space heater?** Yeah, we've all been there.

FrameChecky is a lightweight, native Android app built for gamers who are tired of guessing. Instead of just reading the specs sheet and lying to you, it actually looks at what your phone can do—reads your CPU, checks how hot it's getting, and tells you straight up if you can run a game and at what FPS.

## Why I built this
I was sick of seeing apps that just compare "RAM: 8GB vs 4GB" and give you a fake answer. A phone with 8GB of RAM could still choke on a game if the CPU is throttling. So I built this natively in Kotlin to actually read the hardware, check the thermal status, and estimate the real performance. No web wrappers, no Electron, no lag. Just pure native performance.

## What it actually does
*   **Reads your real specs:** Goes deep into `/proc/cpuinfo`, checks usable RAM (not the fake total), and measures storage speed.
*   **Thermal check:** Reads your battery temp. If your phone is running hot, it'll warn you that your FPS is about to drop.
*   **Simulates settings:** Move sliders for graphics and FPS to see how the game will actually perform.
*   **Community votes:** Log in with Google, and see what other people with your exact device are saying. (Powered by Firebase)
*   **Custom games:** If your game isn't on the list, add it yourself with your own specs.
*   **Clean UI:** Strict dark mode. No flashy gradients, no clutter. Just the data you need.

## How to build it
If you want to compile it yourself (or help me fix bugs):
1. Clone the repo.
2. Grab your own `google-services.json` from Firebase (you need to set up Auth and Firestore).
3. Open it in Android Studio (or Android Code Studio if you're building on the go).
4. Build and run.

*Note: This is a passion project in active development. Things might break, but I'm working on it.*

## Contributing
If you find a bug or have an idea for a feature, just open an issue. I'm open to PRs too.

## License
MIT License. Do whatever you want with it.

---
*Built with Kotlin and too much coffee.*
