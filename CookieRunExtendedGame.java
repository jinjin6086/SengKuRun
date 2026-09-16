import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.awt.Image;
import javax.swing.ImageIcon;




// 1. 메인 실행 창
public class CookieRunExtendedGame extends JFrame {
   public CookieRunExtendedGame() {
       setTitle("쿠키런 리뉴얼 - 애니메이션 & 밸런스 패치");
       setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
       setUndecorated(true);
       GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
       GraphicsDevice device = env.getDefaultScreenDevice();
       device.setFullScreenWindow(this);




       add(new GamePanel());
   }




   public static void main(String[] args) {
       SwingUtilities.invokeLater(() -> new CookieRunExtendedGame().setVisible(true));
   }
}




// 2. 게임 메인 로직 패널
class GamePanel extends JPanel implements ActionListener, KeyListener, MouseListener {
   private static final int WIDTH = 1000;
   private static final int HEIGHT = 600;
   private static final int FPS = 60;
   private static final int PLAYER_X = 150;




   private static final double BASE_GRAVITY = 1.4;
   private static final double BASE_JUMP_VELOCITY = -20.5;
   private static final double BASE_SCROLL_SPEED = 9.0;




   private int gameState = 0;
   private int deadRound = 1;
   private int frameCount = 0;
   private double health = 5000.0;
   private final double MAX_HEALTH = 5000.0;
   private double scrollSpeed = BASE_SCROLL_SPEED;




   private int invincibilityTimer = 0;
   private int confusionTimer = 0;
   private int potionCount = 3;




   private boolean downPressed = false;
   private double soupX = 1000;
   private boolean isReversed = false;
   private int reverseTimer = 0;




   // 🔥 [애니메이션용 변수] 패럴랙스 배경 스크롤용 변수
   private double bgScrollFar = 0;
   private double bgScrollClose = 0;




   // 🔥 [애니메이션용 변수] 피격 시 화면 흔들림(Camera Shake) 타이머
   private int shakeTimer = 0;




   private Player player;
   private List<Platform> platforms = new ArrayList<>();
   private List<Obstacle> obstacles = new ArrayList<>();
   private List<Spike> spikes = new ArrayList<>();
   private List<BossAttack> bossAttacks = new ArrayList<>();




   private double midBossY = 200;
   private double midBossHP = 100.0;
   private int midBossAttackCooldown = 0;




   private double bossHP = 100.0;
   private int bossAttackCooldown = 0;
   private double pagweokX = 720;
   private double pagweokY = 80;
   private int bossHitStack = 0;
   private int bossRushTimer = 0;




   private int shieldTimer = 0;
   private int shieldCooldown = 1200;




   private Rectangle[] endingCardBounds = new Rectangle[3];
   private String[] cardNames = {"평화로운 일상 포토카드", "파괴옥 흑화 포토카드", "새로운 모험 포토카드"};
   private String[] cardDescs = {"김스프와 수프를 먹는 따뜻한 일상!", "어둠의 힘을 물려받은 압도적 포스!", "미지의 세계를 향한 듬직한 뒷모습!"};
   private int selectedEnding = -1;




   private Timer timer;
   private Random random = new Random();
   private int framesSinceLastObstacle = 0;
   private int framesSinceLastVoid = 0;




   private final GradientPaint bgPaint = new GradientPaint(0, 0, new Color(15, 10, 35), 0, HEIGHT, new Color(50, 30, 75));




   // 🔥 [최적화] 매 프레임 AffineTransform 객체 생성을 막기 위한 재사용 변수
   private final AffineTransform savedTransform = new AffineTransform();




   // 🔥 [이미지 추가] 보스 및 김스프 이미지 변수 선언
   private static Image pagweokImg;
   private static Image sangjinImg;
   private static Image soupImg;




   // 🔥 [이미지 추가] 클래스 로드 시 이미지 불러오기
   static {
       try {
           pagweokImg = new ImageIcon("src/enemy3.png").getImage();
           sangjinImg = new ImageIcon("src/enemy4.png").getImage();
           soupImg = new ImageIcon("src/enemy5.png").getImage();
       } catch (Exception e) {
           System.out.println("캐릭터 이미지 로드 실패: " + e.getMessage());
       }
   }




   public GamePanel() {
       setPreferredSize(new Dimension(WIDTH, HEIGHT));
       setFocusable(true);
       addKeyListener(this);
       addMouseListener(this);




       initializeEntities();
       timer = new Timer(1000 / FPS, this);
       timer.start();
   }




   private void initializeEntities() {
       player = new Player(PLAYER_X, HEIGHT - 200, BASE_GRAVITY, BASE_JUMP_VELOCITY);
       platforms.clear();
       obstacles.clear();
       spikes.clear();
       bossAttacks.clear();




       soupX = 1000;
       scrollSpeed = BASE_SCROLL_SPEED;
       downPressed = false;
       health = MAX_HEALTH;
       confusionTimer = 0;
       potionCount = 3;




       pagweokX = 720;
       pagweokY = 80;
       bossHitStack = 0;
       bossRushTimer = 0;
       framesSinceLastVoid = 0;
       shieldTimer = 0;
       shieldCooldown = 1200;
       bgScrollFar = 0;
       bgScrollClose = 0;
       shakeTimer = 0;




       for (int i = 0; i < WIDTH / 50 + 4; i++) {
           platforms.add(new Platform(i * 50, HEIGHT - 100, 50, 50));
       }
   }




   @Override
   public void actionPerformed(ActionEvent e) {
       updateGame();
       repaint();
   }




   private void updateGame() {
       if (reverseTimer > 0) { reverseTimer--; if(reverseTimer == 0) isReversed = false; }
       if (gameState == 0 || gameState == 4 || gameState == 5 || gameState == 7) return;




       frameCount++;
       framesSinceLastObstacle++;
       framesSinceLastVoid++;




       if (invincibilityTimer > 0) invincibilityTimer--;
       if (confusionTimer > 0) confusionTimer--;
       if (shakeTimer > 0) shakeTimer--;




       bgScrollFar = (bgScrollFar + scrollSpeed * 0.15) % WIDTH;
       bgScrollClose = (bgScrollClose + scrollSpeed * 0.4) % WIDTH;




       health -= 0.5;




       if (gameState < 6) scrollSpeed = BASE_SCROLL_SPEED;
       if (downPressed) player.startSlide();




       if (gameState == 1) {
           if (frameCount >= 900) { gameState = 2; frameCount = 0; invincibilityTimer = 60; }
       } else if (gameState == 2) {
           midBossHP = ((1350.0 - frameCount) / 1350.0) * 100.0;
           midBossY += (player.getY() - midBossY) * 0.05;
           midBossAttackCooldown++;




           if (midBossAttackCooldown >= 100) {
               bossAttacks.add(new BossAttack(750, midBossY + 20, 30, 30, 1));
               midBossAttackCooldown = 0;
           }




           if (frameCount >= 1350) {
               gameState = 3; frameCount = 0; invincibilityTimer = 60;
               shieldCooldown = 1200; shieldTimer = 0;
           }
       } else if (gameState == 3) {
           bossHP = ((2400.0 - frameCount) / 2400.0) * 100.0;




           if (shieldTimer > 0) {
               shieldTimer--;
               if (shieldTimer <= 0) shieldCooldown = 1200;
           } else if (shieldCooldown > 0) {
               shieldCooldown--;
               if (shieldCooldown <= 0) shieldTimer = 600;
           }




           if (bossRushTimer > 0) {
               bossRushTimer--;
               if (bossRushTimer > 30) {
                   pagweokX += (player.getX() - pagweokX) * 0.2;
                   pagweokY += (player.getY() - pagweokY) * 0.2;
               } else if (bossRushTimer == 30) {
                   health -= 600;
                   invincibilityTimer = 60;
                   shakeTimer = 15;
               } else {
                   pagweokX += (720 - pagweokX) * 0.1;
                   pagweokY += (80 - pagweokY) * 0.1;
               }
           } else {
               pagweokX += (720 - pagweokX) * 0.05;
               pagweokY += (80 - pagweokY) * 0.05;




               bossAttackCooldown++;
               if (bossAttackCooldown >= 100) {
                   int numAttacks = 2;
                   for (int i = 0; i < numAttacks; i++) {
                       // 하늘에서 떨어지는 공격만 확률적으로 생성하고, 바닥 파란 공 생성 코드는 완전히 삭제!
                       if (random.nextBoolean()) {
                           bossAttacks.add(new BossAttack(random.nextInt(400) + 400 + (i * 60), -random.nextInt(150), 35, 35, 2));
                       }
                   }
                   bossAttackCooldown = 0;
               }
           }




           if (frameCount >= 2400) {
               gameState = 6; frameCount = 0;
               bossAttacks.clear(); obstacles.clear(); spikes.clear();
           }
       } else if (gameState == 6) {
           if (soupX > 600) soupX -= scrollSpeed;
           else scrollSpeed = 0;




           if (frameCount >= 180) {
               gameState = 4; frameCount = 0;
               endingCardBounds[0] = new Rectangle(120, 220, 220, 260);
               endingCardBounds[1] = new Rectangle(390, 220, 220, 260);
               endingCardBounds[2] = new Rectangle(660, 220, 220, 260);
           }
       }




       player.update();




       boolean onPlatform = false;
       for (Platform p : platforms) {
           if (player.intersects(p) && player.getVerticalVelocity() >= 0) {
               if (player.getY() + player.getHeight() - player.getVerticalVelocity() <= p.getY() + 20) {
                   player.setOnGround(true, p.getY());
                   onPlatform = true; break;
               }
           }
       }
       if (!onPlatform) player.setOnGround(false, 0);




       if (player.getY() > HEIGHT) health = 0;




       for (Platform p : platforms) p.moveLeft(scrollSpeed);
       for (Obstacle o : obstacles) o.update(scrollSpeed, player.getX(), frameCount);
       for (Spike s : spikes) s.update(scrollSpeed);
       for (BossAttack ba : bossAttacks) ba.update(scrollSpeed, frameCount);




       double rightmostX = platforms.isEmpty() ? 0 : platforms.get(platforms.size() - 1).getX();
       while (rightmostX < WIDTH + 100) {
           if (gameState > 1 && gameState < 6 && rightmostX > 600 && framesSinceLastVoid >= 1200) {
               rightmostX += 130;
               framesSinceLastVoid = 0;
           }
           platforms.add(new Platform(rightmostX + 50, HEIGHT - 100, 50, 50));
           rightmostX += 50;
       }




       if (gameState < 6) {
           spawnObjects();
           checkCollisions();
       }




       if (health <= 0 && gameState < 6) { deadRound = gameState; gameState = 7; }




       platforms.removeIf(p -> p.getX() + p.getWidth() < 0);
       obstacles.removeIf(o -> o.getX() + o.getWidth() < 0);
       spikes.removeIf(s -> s.getX() + s.getWidth() < 0);
       bossAttacks.removeIf(ba -> ba.getX() + ba.getWidth() < 0 || ba.getY() > HEIGHT);
   }




   private void spawnObjects() {
       int spawnInterval = (gameState == 1) ? 80 : (gameState == 2 ? 60 : 45);
       if (framesSinceLastObstacle >= spawnInterval) {
           if (random.nextInt(10) < 7) {
               double spawnX = WIDTH + 50;
               if (gameState == 1) {
                   if (random.nextBoolean()) obstacles.add(new Obstacle(spawnX, HEIGHT - 185, 45, 45, true, false));
                   else obstacles.add(new Obstacle(spawnX, HEIGHT - 145, 45, 45, false, false));
               } else {
                   int randType = random.nextInt(10);
                   if (randType < 2) obstacles.add(new Obstacle(spawnX, HEIGHT - 185, 45, 45, true, false));
                   else if (randType < 4) obstacles.add(new Obstacle(spawnX, HEIGHT - 145, 45, 45, false, false));
                   else if (randType < 8) obstacles.add(new Obstacle(spawnX, HEIGHT - 190, 40, 90, false, true));
                   else spikes.add(new Spike(spawnX, HEIGHT - 120, 100, 20));
               }
               framesSinceLastObstacle = 0;
           }
       }
   }




   private void checkCollisions() {
       boolean hitThisFrame = false;
       Iterator<Obstacle> obsIter = obstacles.iterator();
       while (obsIter.hasNext()) {
           Obstacle o = obsIter.next();
           if (player.intersects(o)) {
               if (invincibilityTimer == 0) {
                   invincibilityTimer = 70;
                   shakeTimer = 10;
                   if (gameState == 3 && shieldTimer > 0) health -= 200;
                   else health -= 400;
                   hitThisFrame = true;
               }
               obsIter.remove();
           }
       }




       for (Spike s : spikes) {
           if (player.intersects(s)) {
               health -= 6.0;
               if (frameCount % 6 == 0) shakeTimer = 2;
           }
       }




       Iterator<BossAttack> atkIter = bossAttacks.iterator();
       while (atkIter.hasNext()) {
           BossAttack ba = atkIter.next();
           if (player.intersects(ba)) {
               if (ba.getType() == 1) {
                   confusionTimer = 180;
                   atkIter.remove();
               } else if (invincibilityTimer == 0 && !hitThisFrame) {
                   health -= 250;
                   invincibilityTimer = 60;
                   shakeTimer = 12;
                   atkIter.remove();




                   if (gameState == 3 && bossRushTimer == 0 && shieldTimer <= 0) {
                       bossHitStack++;
                       if (bossHitStack >= 2) { bossRushTimer = 60; bossHitStack = 0; }
                   }
               }
           }
       }
   }




   @Override
   protected void paintComponent(Graphics g) {
       super.paintComponent(g);
       if (isReversed && reverseTimer > 0) {
           g.setColor(Color.RED);
           g.setFont(new Font("맑은 고딕", Font.BOLD, 24));
           g.drawString("⚠️ 환각 지속: " + (reverseTimer / 60) + "초 ⚠️", 250, 50);
       }
       Graphics2D g2d = (Graphics2D) g;
       g2d.scale((double) getWidth() / 1000.0, (double) getHeight() / 600.0);
       g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);




       if (shakeTimer > 0 && gameState > 0 && gameState < 4) {
           int shakeX = random.nextInt(shakeTimer + 1) - (shakeTimer / 2);
           int shakeY = random.nextInt(shakeTimer + 1) - (shakeTimer / 2);
           g2d.translate(shakeX, shakeY);
       }




       g2d.setPaint(bgPaint);
       g2d.fillRect(0, 0, WIDTH, HEIGHT);




       drawParallaxBackground(g2d);




       for (Platform p : platforms) p.draw(g2d);
       for (Spike s : spikes) s.draw(g2d);
       for (Obstacle o : obstacles) o.draw(g2d, frameCount, savedTransform);
       for (BossAttack ba : bossAttacks) ba.draw(g2d, frameCount, savedTransform);




       if (gameState == 6) {
           int soupHover = (int)(Math.sin(frameCount * 0.15) * 6);
           // 🔥 [이미지 추가] 김스프 그리기 (이미지 없으면 기존 네모)
           if (soupImg != null && soupImg.getWidth(null) > 0) {
               g2d.drawImage(soupImg, (int)soupX, HEIGHT - 120 + soupHover, 60, 40, null);
           } else {
               g2d.setColor(new Color(255, 200, 50));
               g2d.fillRoundRect((int)soupX, HEIGHT - 120 + soupHover, 60, 40, 20, 20);
           }




           g2d.setColor(Color.WHITE);
           g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
           g2d.drawString("김스프", (int)soupX + 6, HEIGHT - 130 + soupHover);




           if (soupX <= 600) {
               g2d.setColor(Color.CYAN);
               g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 35));
               g2d.drawString("드디어 김스프를 찾았다!", WIDTH / 2 - 200, HEIGHT / 2 - 50);
           }
       }




       if (!(invincibilityTimer > 0 && System.currentTimeMillis() % 140 < 70)) {
           player.draw(g2d, frameCount);
       }




       if (gameState == 3) {
           double pulse = 1.0 + Math.sin(frameCount * 0.1) * 0.05;
           int radiusW = (int)((player.getWidth() + 30) * pulse);
           int radiusH = (int)((player.getHeight() + 30) * pulse);
           int offsetX = (radiusW - player.getWidth()) / 2;
           int offsetY = (radiusH - player.getHeight()) / 2;




           if (shieldTimer > 0) {
               g2d.setColor(new Color(0, 255, 255, 60));
               g2d.fillOval((int)player.getX() - offsetX, (int)player.getY() - offsetY, radiusW, radiusH);
               g2d.setColor(Color.CYAN);
               g2d.setStroke(new BasicStroke(2));
               g2d.drawOval((int)player.getX() - offsetX, (int)player.getY() - offsetY, radiusW, radiusH);




               g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
               g2d.drawString("보호구 ON: " + (shieldTimer / 60) + "초", (int)player.getX() - 25, (int)player.getY() - 25);
           } else {
               g2d.setColor(Color.LIGHT_GRAY);
               g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
               g2d.drawString("보호구 준비중: " + (shieldCooldown / 60) + "초", (int)player.getX() - 25, (int)player.getY() - 25);
           }
       }




       if (confusionTimer > 0 && gameState < 6) {
           g2d.setColor(Color.MAGENTA);
           g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 22));
           // 현재 프레임을 초 단위로 환산 (남은 시간 계산)
           int secondsLeft = (int) Math.ceil(confusionTimer / 60.0);
           g2d.drawString("😵 혼란! 조작 반전! (" + secondsLeft + "초 남음)", (int)player.getX() - 60, (int)player.getY() - 20);
       }




       if (gameState == 2) {
           int bossHover = (int)(Math.sin(frameCount * 0.08) * 12);
           // 🔥 [이미지 추가] 이상진 그리기 (이미지 없으면 기존 네모)
           if (sangjinImg != null && sangjinImg.getWidth(null) > 0) {
               g2d.drawImage(sangjinImg, 750, (int)midBossY + bossHover, 55, 65, null);
           } else {
               g2d.setColor(new Color(150, 50, 250));
               g2d.fillRect(750, (int)midBossY + bossHover, 55, 65);
           }




           g2d.setColor(Color.WHITE);
           g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
           g2d.drawString("이상진", 750, (int)midBossY + bossHover - 20);
           g2d.setColor(Color.DARK_GRAY);
           g2d.fillRect(720, (int)midBossY + bossHover - 10, 110, 8);
           g2d.setColor(new Color(180, 80, 255));
           g2d.fillRect(720, (int)midBossY + bossHover - 10, (int)(110 * (midBossHP / 100.0)), 8);
       } else if (gameState == 3) {
           savedTransform.setTransform(g2d.getTransform());
           double sizePulse = 1.0 + Math.sin(frameCount * 0.12) * 0.06;
           g2d.translate(pagweokX + 70, pagweokY + 70);
           g2d.scale(sizePulse, sizePulse);




           // 🔥 [이미지 추가] 파괴옥 그리기 (이미지 없으면 기존 원)
           if (pagweokImg != null && pagweokImg.getWidth(null) > 0) {
               g2d.drawImage(pagweokImg, -70, -70, 140, 140, null);
           } else {
               g2d.setColor(new Color(230, 30, 30));
               g2d.fillOval(-70, -70, 140, 140);
           }
           g2d.setTransform(savedTransform);




           g2d.setColor(Color.WHITE);
           g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
           g2d.drawString("최종보스 ★파괴옥★", (int)pagweokX - 5, (int)pagweokY - 35);
           g2d.setColor(Color.YELLOW);
           g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
           g2d.drawString("분노 스택: " + bossHitStack + " / 2", (int)pagweokX + 15, (int)pagweokY - 15);
           g2d.setColor(Color.DARK_GRAY);
           g2d.fillRect((int)pagweokX, (int)pagweokY + 150, 140, 12);
           g2d.setColor(Color.ORANGE);
           g2d.fillRect((int)pagweokX, (int)pagweokY + 150, (int)(140 * (bossHP / 100.0)), 12);
       }




       if (gameState > 0 && gameState < 4 || gameState == 6) {
           g2d.setColor(Color.DARK_GRAY);
           g2d.fillRect(50, 20, 350, 25);
           g2d.setColor(Color.RED);
           g2d.fillRect(50, 20, (int)(350 * Math.max(0, health) / MAX_HEALTH), 25);
           g2d.setColor(Color.WHITE);
           g2d.drawRect(50, 20, 350, 25);
           g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
           g2d.drawString("HP", 15, 38);
           g2d.setColor(Color.GREEN);
           g2d.drawString("물약(Q): " + potionCount + "개", 420, 38);
           g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 22));
           g2d.setColor(Color.CYAN);
           if (gameState == 6) g2d.drawString("엔딩 연출 중...", WIDTH - 180, 38);
           else g2d.drawString("ROUND " + gameState, WIDTH - 160, 38);
       }




       if (gameState == 0) drawStartScreen(g2d);
       else if (gameState == 4) drawCardSelectionScreen(g2d);
       else if (gameState == 5) drawFinalEndingScreen(g2d);
       else if (gameState == 7) drawGameOverScreen(g2d);
   }




   private void drawParallaxBackground(Graphics2D g2d) {
       g2d.setColor(new Color(255, 255, 255, 70));
       for (int i = 0; i < 4; i++) {
           int x = (int) ((i * 300 - bgScrollFar + WIDTH) % WIDTH);
           g2d.fillRect(x, 50 + (i * 40), 3, 3);
           g2d.fillRect((x + 150) % WIDTH, 200 + (i * 30), 2, 2);
       }
       g2d.setColor(new Color(200, 220, 255, 140));
       for (int i = 0; i < 3; i++) {
           int x = (int) ((i * 450 - bgScrollClose + WIDTH) % WIDTH);
           g2d.fillOval(x, 80 + (i * 70), 6, 6);
       }
   }




   private void drawStartScreen(Graphics2D g2d) {
       g2d.setColor(new Color(0, 0, 0, 200)); g2d.fillRect(0, 0, WIDTH, HEIGHT);
       g2d.setColor(Color.ORANGE); g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 45));
       g2d.drawString("“김스프를 구해라! (HARD MODE)”", WIDTH / 2 - 320, 140);
       g2d.setColor(Color.WHITE); g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 20));
       g2d.drawString("=== 조작 방법 설명 ===", WIDTH / 2 - 110, 240);
       g2d.setFont(new Font("Malgun Gothic", Font.PLAIN, 16));
       g2d.drawString("▶ 점프 / 2단 점프 : ↑ (위쪽 화살표)", WIDTH / 2 - 170, 290);
       g2d.drawString("▶ 슬라이딩 다운 : ↓ (아래쪽 화살표) 누르고 있기", WIDTH / 2 - 170, 330);
       g2d.drawString("▶ 긴급 체력 회복 : Q (최대 3번 사용 가능)", WIDTH / 2 - 170, 370);
       g2d.setColor(Color.GREEN); g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
       g2d.drawString("엔터(ENTER)를 누르면 시작합니다!", WIDTH / 2 - 160, 460);
   }




   private void drawGameOverScreen(Graphics2D g2d) {
       g2d.setColor(new Color(30, 0, 0, 220)); g2d.fillRect(0, 0, WIDTH, HEIGHT);
       g2d.setColor(Color.RED); g2d.setFont(new Font("Arial", Font.BOLD, 60));
       g2d.drawString("YOU DIED", WIDTH / 2 - 150, 220);
       g2d.setColor(Color.WHITE); g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 30));
       g2d.drawString("도달한 라운드 : ROUND " + deadRound, WIDTH / 2 - 170, 320);
       g2d.setColor(Color.LIGHT_GRAY); g2d.setFont(new Font("Malgun Gothic", Font.PLAIN, 20));
       g2d.drawString("[엔터(ENTER)를 눌러 재도전]", WIDTH / 2 - 140, 420);
   }




   private void drawCardSelectionScreen(Graphics2D g2d) {
       g2d.setColor(new Color(15, 5, 30, 240)); g2d.fillRect(0, 0, WIDTH, HEIGHT);
       g2d.setColor(Color.CYAN); g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 32));
       g2d.drawString("★ 김스프 구출 완료! 보상 포토카드를 뽑으세요! ★", WIDTH / 2 - 370, 110);
       for (int i = 0; i < 3; i++) {
           Rectangle card = endingCardBounds[i];
           g2d.setColor(new Color(60, 60, 90)); g2d.fillRoundRect(card.x, card.y, card.width, card.height, 15, 15);
           g2d.setColor(Color.LIGHT_GRAY); g2d.drawRoundRect(card.x, card.y, card.width, card.height, 15, 15);
           g2d.setColor(Color.YELLOW); g2d.setFont(new Font("Arial", Font.BOLD, 80));
           g2d.drawString("?", card.x + 85, card.y + 140);
           g2d.setColor(Color.WHITE); g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
           g2d.drawString("클릭하여 뽑기", card.x + 60, card.y + 220);
       }
   }




   private void drawFinalEndingScreen(Graphics2D g2d) {
       g2d.setColor(Color.BLACK); g2d.fillRect(0, 0, WIDTH, HEIGHT);
       g2d.setColor(Color.ORANGE); g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 45));
       g2d.drawString("🎉 포토카드 당첨! 🎉", WIDTH / 2 - 220, 130);
       g2d.setColor(new Color(240, 240, 255)); g2d.fillRoundRect(WIDTH / 2 - 200, 180, 400, 220, 20, 20);
       g2d.setColor(Color.BLUE); g2d.setStroke(new BasicStroke(3)); g2d.drawRoundRect(WIDTH / 2 - 200, 180, 400, 220, 20, 20);
       g2d.setColor(Color.BLACK); g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 28));
       g2d.drawString(cardNames[selectedEnding], WIDTH / 2 - 160, 250);
       g2d.setFont(new Font("Malgun Gothic", Font.PLAIN, 18));
       g2d.drawString(cardDescs[selectedEnding], WIDTH / 2 - 160, 320);
       g2d.setColor(Color.GREEN); g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
       g2d.drawString("[엔터(ENTER)를 눌러 타이틀로 돌아가기]", WIDTH / 2 - 180, 480);
   }




   @Override
   public void mousePressed(MouseEvent e) {
       if (gameState == 4) {
           double scaleX = (double) getWidth() / 1000.0;
           double scaleY = (double) getHeight() / 600.0;
           int virtualX = (int) (e.getX() / scaleX);
           int virtualY = (int) (e.getY() / scaleY);
           for (int i = 0; i < 3; i++) {
               if (endingCardBounds[i].contains(virtualX, virtualY)) { selectedEnding = i; gameState = 5; break; }
           }
       }
   }
   @Override public void mouseClicked(MouseEvent e) {}
   @Override public void mouseReleased(MouseEvent e) {}
   @Override public void mouseEntered(MouseEvent e) {}
   @Override public void mouseExited(MouseEvent e) {}




   @Override
   public void keyPressed(KeyEvent e) {
       if (e.getKeyCode() == KeyEvent.VK_ESCAPE) System.exit(0);
       if (e.getKeyCode() == KeyEvent.VK_Q) {
           if (potionCount > 0 && gameState > 0 && gameState < 6) {
               potionCount--; health = Math.min(MAX_HEALTH, health + (MAX_HEALTH / 5.0));
           }
       }
       if ((gameState == 5 || gameState == 7) && e.getKeyCode() == KeyEvent.VK_ENTER) {
           gameState = 0; initializeEntities(); return;
       }
       if (gameState == 0) {
           if (e.getKeyCode() == KeyEvent.VK_ENTER) { gameState = 1; frameCount = 0; }
           return;
       }




       boolean isConfused = confusionTimer > 0;
       if (e.getKeyCode() == KeyEvent.VK_UP) {
           if (isConfused) downPressed = true;
           else if (!player.isSliding()) player.jump();
       }
       if (e.getKeyCode() == KeyEvent.VK_DOWN) {
           if (isConfused) { if (!player.isSliding()) player.jump(); }
           else downPressed = true;
       }
   }




   @Override
   public void keyReleased(KeyEvent e) {
       if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
           downPressed = false; player.stopSlide();
       }
   }
   @Override public void keyTyped(KeyEvent e) {}
}




// 3. 엔티티 및 게임 오브젝트 클래스
abstract class Entity {
   protected double x, y;
   protected int width, height;
   public Entity(double x, double y, int width, int height) { this.x = x; this.y = y; this.width = width; this.height = height; }
   public void moveLeft(double speed) { x -= speed; }




   public boolean intersects(Entity other) {
       return this.x < other.x + other.width &&
               this.x + this.width > other.x &&
               this.y < other.y + other.height &&
               this.y + this.height > other.y;
   }




   public double getX() { return x; } public double getY() { return y; }
   public void setY(double y) { this.y = y; }
   public int getWidth() { return width; } public int getHeight() { return height; }
}




class Player extends Entity {
   private double vY;
   private double baseGravity;
   private double jumpVelocity;
   private boolean onGround = false;
   private boolean sliding = false;
   private int jumpCount = 0;


   // 🔥 1. 플레이어 이미지 2장을 저장할 변수 추가!
   private static Image playerImg1;
   private static Image playerImg2;


   // 🔥 2. 게임 켤 때 사진 불러오기
   static {
       try {
           playerImg1 = new ImageIcon("src/player1.png").getImage();
           playerImg2 = new ImageIcon("src/player2.png").getImage();
       } catch (Exception e) {
           System.out.println("플레이어 이미지 로드 실패: " + e.getMessage());
       }
   }


   public Player(double x, double y, double baseGravity, double jumpVelocity) {
       super(x, y, 42, 75);
       this.vY = 0; this.baseGravity = baseGravity; this.jumpVelocity = jumpVelocity;
   }


   public void draw(Graphics2D g2d, int frameCount) {
       int drawW = width;
       int drawH = height;
       int drawX = (int)x;
       int drawY = (int)y;


       // 점프 중이거나 슬라이딩 중일 때 캐릭터 모양 찌그러뜨리는 기존 효과 (유지)
       if (!onGround && !sliding) {
           double stretch = Math.min(0.15, Math.abs(vY) * 0.01);
           drawH = (int)(height * (1.0 + stretch));
           drawW = (int)(width * (1.0 - stretch));
           drawX = (int)x + (width - drawW) / 2;
           drawY = (int)y - (drawH - height);
       } else if (sliding) {
           double squish = 1.0 + Math.sin(frameCount * 0.4) * 0.05;
           drawH = (int)(height * squish);
           drawY = (int)y + (height - drawH);
       }


       // 🔥 3. 사진을 번갈아 보여주는 핵심 애니메이션 코드!
       if (playerImg1 != null && playerImg2 != null) {
           // frameCount를 10으로 나눈 몫이 짝수일 때와 홀수일 때를 나눔 (10프레임마다 사진 변경)
           if ((frameCount / 10) % 2 == 0) {
               g2d.drawImage(playerImg1, drawX, drawY, drawW, drawH, null);
           } else {
               g2d.drawImage(playerImg2, drawX, drawY, drawW, drawH, null);
           }
       } else {
           // 만약 사진이 없거나 오류가 났을 때를 대비한 기존 네모 그리기 (안전장치)
           g2d.setColor(sliding ? Color.YELLOW : Color.ORANGE);
           g2d.fillRect(drawX, drawY, drawW, drawH);
           g2d.setColor(Color.WHITE);
           g2d.drawRect(drawX, drawY, drawW, drawH);
       }
   }


   public void update() {
       if (!onGround) {
           vY += baseGravity;
           if (vY > 25.0) vY = 25.0;
           y += vY;
       }
   }


   public void jump() {
       if (onGround || jumpCount < 2) {
           vY = jumpVelocity;
           onGround = false;
           jumpCount++;
       }
   }


   public void startSlide() {
       if (!sliding && onGround) { sliding = true; height = 38; y += 37; }
   }


   public void stopSlide() {
       if (sliding) { sliding = false; y -= 37; height = 75; }
   }


   public void setOnGround(boolean onGround, double groundY) {
       this.onGround = onGround;
       if (onGround) { this.vY = 0; this.y = groundY - height; this.jumpCount = 0; }
   }


   public double getVerticalVelocity() { return vY; }
   public boolean isSliding() { return sliding; }
}




class Platform extends Entity {
   public Platform(double x, double y, int width, int height) { super(x, y, width, height); }
   public void draw(Graphics2D g2d) {
       g2d.setColor(new Color(100, 70, 50)); g2d.fillRect((int)x, (int)y, width, height);
   }
}




class Spike extends Entity {
   public Spike(double x, double y, int width, int height) { super(x, y, width, height); }
   public void update(double scrollSpeed) { x -= scrollSpeed; }
   public void draw(Graphics2D g2d) {
       g2d.setColor(new Color(200, 0, 0));
       for(int i = 0; i < width; i += 10) {
           int[] xPoints = {(int)x + i, (int)x + i + 5, (int)x + i + 10};
           int[] yPoints = {(int)y + height, (int)y, (int)y + height};
           g2d.fillPolygon(xPoints, yPoints, 3);
       }
   }
}




class Obstacle extends Entity {
   private boolean isUpper;
   private boolean isPopup;
   private double targetY;




   private static Image enemyImg1;
   private static Image enemyImg2;




   static {
       try {
           enemyImg1 = new ImageIcon("src/enemy1.png").getImage();
           enemyImg2 = new ImageIcon("src/enemy2.png").getImage();
       } catch (Exception e) {
           System.out.println("이미지 로드 실패: " + e.getMessage());
       }
   }




   public Obstacle(double x, double targetY, int width, int height, boolean isUpper, boolean isPopup) {
       super(x, isPopup ? 600 : targetY, width, height);
       this.isUpper = isUpper;
       this.isPopup = isPopup;
       this.targetY = targetY;
   }




   public void update(double scrollSpeed, double playerX, int frameCount) {
       x -= scrollSpeed;
       if (isPopup && (x - playerX) < 400 && y > targetY) {
           if (y - 25.0 < targetY) y = targetY;
           else y -= 25.0;
       }
   }




   public void draw(Graphics2D g2d, int frameCount, AffineTransform sharedTx) {
       Image imgToDraw = isPopup ? enemyImg2 : enemyImg1;




       if (imgToDraw != null && imgToDraw.getWidth(null) > 0) {
           if (isPopup) {
               g2d.drawImage(imgToDraw, (int)x, (int)y, width, height, null);
           } else {
               sharedTx.setTransform(g2d.getTransform());
               g2d.translate(x + width / 2.0, y + height / 2.0);
               g2d.rotate(frameCount * 0.05);
               g2d.drawImage(imgToDraw, -width / 2, -height / 2, width, height, null);
               g2d.setTransform(sharedTx);
           }
       } else {
           g2d.setColor(isUpper ? Color.PINK : (isPopup ? Color.ORANGE : Color.LIGHT_GRAY));
           g2d.fillRect((int)x, (int)y, width, height);
       }
   }
}




class BossAttack extends Entity {
   private int type;
   public BossAttack(double x, double y, int width, int height, int type) {
       super(x, y, width, height); this.type = type;
   }
   public int getType() { return type; }
   public void update(double scrollSpeed, int frameCount) {
       if (type == 1) x -= (scrollSpeed + 4.0);
       else if (type == 2) { x -= scrollSpeed; y += 5.5; }
       else if (type == 3) x -= scrollSpeed;
   }




   public void draw(Graphics2D g2d, int frameCount, AffineTransform sharedTx) {
       if (type == 1) {
           g2d.setColor(Color.BLACK); g2d.fillOval((int)x, (int)y, width, height);
           g2d.setColor(Color.WHITE); g2d.drawOval((int)x, (int)y, width, height);
       } else if (type == 2) {
           g2d.setColor(Color.YELLOW); g2d.fillRect((int)x, (int)y, width, height);
           g2d.setColor(Color.RED); g2d.drawRect((int)x, (int)y, width, height);
       } else if (type == 3) {
           g2d.setColor(Color.CYAN); g2d.fillOval((int)x, (int)y, width, height);
           g2d.setColor(Color.BLUE); g2d.drawOval((int)x, (int)y, width, height);
       }
   }
}









