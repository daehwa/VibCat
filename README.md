# VibCat : Vibration Categorization for Input & Interaction

----
## VibCat?
The name VibCat is inspired by [RadarCat](https://sachi.cs.st-andrews.ac.uk/research/interaction/radarcat-exploits-googles-soli-radar-sensor-for-object-and-material-recognition/) : _Radar Categorization for Input & Interaction_ of st-andrews' HCI research group. The __VibCat__ is _'Vibration Categorization for Input & Interaction'_.


[demo video](https://youtu.be/D0591qFnU5k) is now available.

----
## How it works
When our smartphone ringing(this case vibrating in silent mode), We can hear different vibrating sound. This because of rigidity of object. I use accelerometer on usual smart phone and measure the value. The different amplitudes are observed in different objects. 
 
### Vibration on the desk

![vibration-desk](./screenshot/vibration-desk.png)

### Vibration on the Hand

![vibration-hand](./screenshot/vibration-hand.png)

### Vibration with the cup, cup with water and glass bottle
we can see this in [demo video](https://youtu.be/D0591qFnU5k).

----
## Machine learning
# Data preprocessing
1. Collect vibration raw data using accelerometers on mobile devices. Each time device vibrates 1 sec, and collect 20 data (#50 ~ #90)
2. Savitzky golay filter data smoothing
3. Arrange the wave (high peak is in first position)
4. Feature selection analysis :
	- 40 data points + 40 absolute + average + root mean square(rms) + min +max
	- Through feature selection analysis, we found the derived features are highly ranked.
5.  Train with Softmax classifier 



----
## Result
# Accuracy
# Classification
 
### VibCat for the desk

![VibCat-desk](./screenshot/VibCat-desk.png)

### VibCat for the hand

![VibCat-hand](./screenshot/VibCat-hand.png)

----
## Future work
* Root the kernel and control the vibration intensity. Then collect the various channel’s result and train it.
* Extend the accelerometer measure region.
* Control the vibration intensity. Then find the natural period directly.
* [ViBand](https://dl.acm.org/citation.cfm?id=2984582)


