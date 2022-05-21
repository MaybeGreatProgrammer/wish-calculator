<p align="center">
  <img src="Icon.png" height="192">
  <h1 align="center">Wish Calculator</h1>

Wish Calculator is an Android app that aims to give users information on the probabilities involved in the wishing process. The application currently has two modes: Results and Saving. Both modes take into account your current **pity** and **guarantee**, and run a total amount of simulations equal to the **sample size**. All processing is done on-device.

# Simulation Basics

The simulation is based on official information and user data from [Paimon.moe](https://paimon.moe/wish/tally?id=300021). The base chance to get any 5 star is **0.6%**. Starting from Wish #1 as Pity 1, the probability stays the same until Wish #74, where it **increases linearly to 100%** at Wish #90. The 50/50 is also taken into account, and standard 5 star characters are not counted due to being largely undesirable.

# Modes

## Results

Takes the amount of **wishes** you want to spend as input, and outputs a distribution of possible outcomes and their frequency - how many limited *event* 5 star characters you are likely to get. 

### Example

Copies Pulled | Chance | Total Chance
--------------|--------|-------------
1|62.08%|100.00%
2|33.38%|37.92%
3|4.17%|4.54%
4|0.35%|0.37%
5|0.02%|0.02%

*Wishes: 135 Pity: 35 Guarantee: No Samples: 10K*

## Saving

Takes the amount of **event 5 star pulls** (e.g. 1 for C0, 3 for C2) you want as input, and outputs the probability of achieving your goal as you spend more fates. This is based on a distribution of wishes spent to hit the target. The UI then presents the amount of wishes needed to hit common probability thresholds (e.g. 50%, 90%, 99%), and allows you to input your own.

### Example

Wishes|Chance
---------|---
1|18.2%
2|37.63%
3|55.99%
4|71.7%
5|82.64%
6|90.89%
7|95.65%
8|98.42%
9|99.48%
10|99.82%
11|99.98%
12|100%

*Pulls: 1 Pity: 75 Guarantee: Yes Samples: 10K*

# Is the Code Good?

No, this is  my 2nd Jetpack Compose project. I definitely violated *all* of the best practices of both Kotlin and Compose while writing it. It's all in 2 monolithic files, and the entirety of the data is crammed into the UI State object. If you can optimize the simulation or make the code slightly less of a mess, open a pull request, or fork it if you don't want to contribute to the APK I publish on Google Play (which is ad-free because I have no idea how to implement those either). Thanks, and have fun!