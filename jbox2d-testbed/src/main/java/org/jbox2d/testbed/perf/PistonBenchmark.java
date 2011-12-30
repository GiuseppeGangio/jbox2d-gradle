package org.jbox2d.testbed.perf;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.PrismaticJoint;
import org.jbox2d.dynamics.joints.PrismaticJointDef;
import org.jbox2d.dynamics.joints.RevoluteJoint;
import org.jbox2d.dynamics.joints.RevoluteJointDef;

/**
 * Benchmark - piston example (constantly bumping a bunch of circles and boxes). Should be a decent
 * mix of circle and polygon collisions/contacts, though very little joint work.
 * 
 * NOTE: some iterations cause objects to fall through the piston
 * 
 * Test Name               Milliseconds Avg      FPS (optional)
 * Pistons (bullets)              2071.3008            386.2307
 * Pistons (no bullets)           1528.2571            523.4721
 * 
 * @author eric, Daniel
 * 
 */
public class PistonBenchmark extends PerfTest {
  public static int iters = 10;
  public static int frames = 800;
  public static float timeStep = 1f/60;
  public static int velIters = 8;
  public static int posIters = 3;

  public RevoluteJoint m_joint1;
  public PrismaticJoint m_joint2;

  public PistonBenchmark() {
    super(2, iters);
  }

  public static void main(String[] args) {
    PistonBenchmark benchmark = new PistonBenchmark();
    benchmark.go();
  }

  @Override
  public void runTest(int argNum) {
    boolean bullets = argNum == 0;
    
    World world = new World(new Vec2(0.0f, -10.0f), true);
    Body ground = null;
    {
      BodyDef bd = new BodyDef();
      ground = world.createBody(bd);

      PolygonShape shape = new PolygonShape();
      shape.setAsBox(1.0f, 100.0f);
      bd = new BodyDef();
      bd.type = BodyType.STATIC;
      FixtureDef sides = new FixtureDef();
      sides.shape = shape;
      sides.density = 0;
      sides.friction = 0;
      sides.filter.categoryBits = 4;
      sides.filter.maskBits = 2;

      bd.position.set(-6.01f, 50.0f);
      Body bod = world.createBody(bd);
      bod.createFixture(sides);
      bd.position.set(6.01f, 50.0f);
      bod = world.createBody(bd);
      bod.createFixture(sides);
    }

    {
      Body prevBody = ground;

      // Define crank.
      {
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.5f, 2.0f);

        BodyDef bd = new BodyDef();
        bd.type = BodyType.DYNAMIC;
        bd.position.set(0.0f, 7.0f);
        Body body = world.createBody(bd);
        body.createFixture(shape, 2.0f);

        RevoluteJointDef rjd = new RevoluteJointDef();
        rjd.initialize(prevBody, body, new Vec2(0.0f, 5.0f));
        rjd.motorSpeed = 1.0f * MathUtils.PI;
        rjd.maxMotorTorque = 20000;
        rjd.enableMotor = true;
        m_joint1 = (RevoluteJoint) world.createJoint(rjd);

        prevBody = body;
      }

      // Define follower.
      {
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.5f, 4.0f);

        BodyDef bd = new BodyDef();
        bd.type = BodyType.DYNAMIC;
        bd.position.set(0.0f, 13.0f);
        Body body = world.createBody(bd);
        body.createFixture(shape, 2.0f);

        RevoluteJointDef rjd = new RevoluteJointDef();
        rjd.initialize(prevBody, body, new Vec2(0.0f, 9.0f));
        rjd.enableMotor = false;
        world.createJoint(rjd);

        prevBody = body;
      }

      // Define piston
      {
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(7f, 1.5f);

        BodyDef bd = new BodyDef();
        bd.type = BodyType.DYNAMIC;
        bd.position.set(0.0f, 17.0f);
        Body body = world.createBody(bd);
        FixtureDef piston = new FixtureDef();
        piston.shape = shape;
        piston.density = 2;
        piston.filter.categoryBits = 1;
        piston.filter.maskBits = 2;
        body.createFixture(piston);

        RevoluteJointDef rjd = new RevoluteJointDef();
        rjd.initialize(prevBody, body, new Vec2(0.0f, 17.0f));
        world.createJoint(rjd);

        PrismaticJointDef pjd = new PrismaticJointDef();
        pjd.initialize(ground, body, new Vec2(0.0f, 17.0f), new Vec2(0.0f, 1.0f));

        pjd.maxMotorForce = 1000.0f;
        pjd.enableMotor = true;

        m_joint2 = (PrismaticJoint) world.createJoint(pjd);
      }

      // Create a payload
      {
        PolygonShape sd = new PolygonShape();
        BodyDef bd = new BodyDef();
        bd.type = BodyType.DYNAMIC;
        FixtureDef fixture = new FixtureDef();
        Body body;
        for (int i = 0; i < 100; ++i) {
          sd.setAsBox(0.4f, 0.3f);
          bd.position.set(-1.0f, 23.0f + i);

          bd.bullet = bullets;
          body = world.createBody(bd);
          fixture.shape = sd;
          fixture.density = .1f;
          fixture.filter.categoryBits = 2;
          fixture.filter.maskBits = 1 | 4 | 2;
          body.createFixture(fixture);
        }

        CircleShape cd = new CircleShape();
        cd.m_radius = 0.36f;
        for (int i = 0; i < 100; ++i) {
          bd.position.set(1.0f, 23.0f + i);
          bd.bullet = bullets;
          fixture.shape = cd;
          fixture.density = 2f;
          fixture.filter.categoryBits = 2;
          fixture.filter.maskBits = 1 | 4 | 2;
          body = world.createBody(bd);
          body.createFixture(fixture);
        }
      }
    }


    for (int i = 0; i < frames; i++) {
      world.step(timeStep, posIters, velIters);
    }
  }

  @Override
  public String getTestName(int argNum) {
    return "Pistons " + (argNum == 0 ? "(bullets)" : "(no bullets)");
  }
  
  @Override
  public int getFrames(int testNum) {
    return frames;
  }
}