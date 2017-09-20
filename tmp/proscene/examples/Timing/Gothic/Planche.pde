class Planche extends Interpolation {
  float dx, dy, dz;
  int type;
  
  Planche(PVector b, Quat qb, float dx, float dy, float dz, int t) {
    super(b, qb);
    this.dx=dx;
    this.dy=dy;
    this.dz=dz;
    this.type=t;
  }
  
  void actualiser() {
    super.dessin(dx, dy, dz, type);
  }
}
