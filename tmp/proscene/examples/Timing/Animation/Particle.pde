class Particle {
  PVector speed;
  PVector pos;
  int age;
  int ageMax;

  public Particle() {
    speed = new PVector();
    pos = new PVector();
    init();
  }

  public void animate() {
    speed.z -= 0.05f;
    pos = PVector.add(pos, PVector.mult(speed, 10f));

    if (pos.z < 0.0) {
      speed.z = -0.8f * speed.z;
      pos.z = 0.0f;
    }

    if (++age == ageMax)
      init();
  }

  public void draw() {		
    stroke( 255 * ((float) age / (float) ageMax), 255 * ((float) age / (float) ageMax), 255);
    vertex(pos.x, pos.y, pos.z);
  }

  public void init() {		
    pos = new PVector(0.0f, 0.0f, 0.0f);
    float angle = 2.0f * PI * random(1);
    float norm = 0.04f * random(1);
    speed = new PVector(norm * cos(angle), norm * sin(angle), random(1));
    age = 0;
    ageMax = 50 + (int) random(100);
  }
}