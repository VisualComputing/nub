class Particle {
  TimingTask task;
  PVector speed;
  PVector pos;
  int age;
  int ageMax;

  Particle() {
    speed = new PVector();
    pos = new PVector();
    init();
    task = new TimingTask(() -> {
      speed.z -= 0.05;
      pos = PVector.add(pos, PVector.mult(speed, 10));
      if (pos.z < 0.0) {
        speed.z = -0.8 * speed.z;
        pos.z = 0;
      }
      if (++age == ageMax)
      init();
    });
    task.run();
  }

  void init() {
    pos = new PVector(0, 0, 0);
    float angle = TWO_PI * random(1);
    float norm = 0.04 * random(1);
    speed = new PVector(norm * cos(angle), norm  * sin(angle), random(1));
    age = 0;
    ageMax = 50 + (int) random(100);
  }

  void draw() {
    stroke( 255 * ((float) age / (float) ageMax), 255 * ((float) age / (float) ageMax), 255);
    vertex(pos.x, pos.y, pos.z);
  }
}
