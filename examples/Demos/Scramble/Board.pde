public class Board {
  private int size; // size (in patches) of the board
  private int moves; // total number of moves
  private PImage img; // image to show for the patches
  private Patch[][] patches; // patches that make up the board

  public Board(int size, PImage img) {
    this.size = size;
    this.img = img;
    order();
  }

  public void clear() {
    if (patches != null)
      for (int i = 0; i < patches.length; i++)
        for (int j = 0; j < patches[0].length; j++)
          scene.pruneBranch(patches[i][j]);
  }

  public void order() {
    clear();
    patches = new Patch[size][size];
    int number = 1; // current patch number
    PImage pimg = null; // current patch image
    for (int y = 0; y < size; y++) // for each row
      for (int x = 0; x < size; x++) // for each column
        if (number < size * size) {
          if (img != null) {
            pimg = createImage(img.width / size, img.height / size, RGB); // a new image for the current patch
            // draw the corresponding image part for the current patch
            pimg.blend(img, x * pimg.width, y * pimg.height, pimg.width, pimg.height, 0, 0, pimg.width, pimg.height, ADD);
          }
          patches[y][x] = new Patch(number, 150f / size, pimg, scene, this); // create the new patch
          number++;
        }
    moves = 0;
  }

  public void update() {
    for (int y = 0; y < size; y++)
      for (int x = 0; x < size; x++)
        if (patches[y][x] != null)
          // recompute the patch appropriate position based on current column and row count (x, y)
          // patches[][].getSize() * x, moves the patch relative to the board position
          // patches[][].getSize * (x - size / 2 + 0.5), centers the board in world space
          patches[y][x].setPosition(patches[y][x].getSize() * ((float) x - (float) size / 2 + 0.5), patches[y][x].getSize() * ((float) y - (float) size / 2 + 0.5), 0);
  }

  public void movePatch(Patch patch) {
    int xp = -1, // column of the patch to move
      yp = -1, // row of the patch to move
      xh = -1, // column of hole in the board
      yh = -1; // row of the hole in the board

    for (int y = 0; y < size; y++)
      for (int x = 0; x < size; x++)
        if (patches[y][x] == patch) { // have we found the patch to move?
          xp = x;
          yp = y;
        } else if (patches[y][x] == null) { // have we found the hole?
          xh = x;
          yh = y;
        }
    if (yp == yh) // row of the patch is the same as the row of the hole?
      movePatchHorizontally(xp, xh, yh);
    else if (xp == xh) // column of the patch is the same as the column of the hole?
      movePatchVertically(yp, yh, xh);
  }

  private void movePatchHorizontally(int xp, int xh, int y) {
    int i = xp < xh ? 1 : -1; // are we going to move left or right?
    if (patches[y][xp + i] == null) { // adjacent patch is a hole?
      swap(xp, y, xp + i, y);
      moves++;
    } else { // no adjacent hole, but we know there must be one, so we move the adjacent patch, and then we move the current one
      movePatchHorizontally(xp + i, xh, y);
      movePatchHorizontally(xp, xh, y);
      moves--; // discount one move, becouse we are moving a set of patches
    }
  }

  private void movePatchVertically(int yp, int yh, int x) {
    int i = yp < yh ? 1 : -1;
    if (patches[yp + i][x] == null) {
      swap(x, yp, x, yp + i);
      moves++;
    } else {
      movePatchVertically(yp + i, yh, x);
      movePatchVertically(yp, yh, x);
      moves--;
    }
  }

  private void swap(int x1, int y1, int x2, int y2) {
    Patch t = patches[y1][x1];
    patches[y1][x1] = patches[y2][x2];
    patches[y2][x2] = t;
  }

  public void scramble() {
    order();
    for (int i = 0; i < size * size * 1000; i++) // iterate far enough to ensure we get a scrambled board
      movePatch(patches[(int) random(0, size)][(int) random(0, size)]); // move a patch randomly
    moves = 0;
  }

  public boolean isOrdered() {
    int max = 0;
    for (int y = 0; y < size; y++)
      for (int x = 0; x < size; x++)
        if (patches[y][x] != null)
          if (patches[y][x].getNumber() > max)
            max = patches[y][x].getNumber();
          else
            return false;
    return patches[size - 1][size - 1] == null; // last patch must be a hole
  }

  public int getMoves() {
    return moves;
  }

  public PImage getImage() {
    return img;
  }

  public void setImage(PImage img) {
    this.img = img;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
    order();
  }
}