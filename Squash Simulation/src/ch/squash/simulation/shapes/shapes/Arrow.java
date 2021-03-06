package ch.squash.simulation.shapes.shapes;

import android.opengl.GLES20;
import ch.squash.simulation.graphic.ShaderType;
import ch.squash.simulation.shapes.common.AbstractShape;
import ch.squash.simulation.shapes.common.IVector;
import ch.squash.simulation.shapes.common.SolidType;
import ch.squash.simulation.shapes.common.Vector;

public class Arrow extends AbstractShape {
	private final static String TAG = Arrow.class.getSimpleName();
	
	public Arrow(final String tag, final float startx, final float starty, final float startz, final float endx,
			final float endy, final float endz, final float[] color) {
		super(tag, startx, starty, startz, ShaderType.NO_LIGHT);
		
		initialize(getVertices(0, 0, 0, endx - startx, endy - starty, endz - startz), getColorData(color), new float[0],
				GLES20.GL_LINES, SolidType.NONE, null);
	}
	
	public static float[] getVertices(final float startx, final float starty,
			final float startz, final float endx, final float endy, final float endz) {
		// must be public static for PhysicalVector!
		
		final float[] vertices = new float[10 * 3];

		final IVector v = new Vector(endx - startx, endy - starty, endz - startz).multiply(0.05f);
		
		final IVector[] o = new IVector[4];
		
		// find component with greatest absolute value
		int maxComp = 0;
		for (int i = 1; i < 3; i++)
			if (Math.abs(v.getDirection()[i]) > Math.abs(v.getDirection()[maxComp]))
				maxComp = i;
		
		if (areEqual(v.getLength(), Math.abs(v.getDirection()[maxComp])
				)){
			// if we have exactly one non-zero component, use trivial orthogonal vectors
			if (maxComp == 0){
				o[0] = new Vector(0, v.getDirection()[maxComp], 0);
				o[1] = new Vector(0, -v.getDirection()[maxComp], 0);
				o[2] = new Vector(0, 0, v.getDirection()[maxComp]);
				o[3] = new Vector(0, 0, -v.getDirection()[maxComp]);
			}else if (maxComp == 1){
				o[0] = new Vector(v.getDirection()[maxComp], 0, 0);
				o[1] = new Vector(-v.getDirection()[maxComp], 0, 0);
				o[2] = new Vector(0, 0, v.getDirection()[maxComp]);
				o[3] = new Vector(0, 0, -v.getDirection()[maxComp]);
			}else if (maxComp == 2){
				o[0] = new Vector(v.getDirection()[maxComp], 0, 0);
				o[1] = new Vector(-v.getDirection()[maxComp], 0, 0);
				o[2] = new Vector(0, v.getDirection()[maxComp], 0);
				o[3] = new Vector(0, -v.getDirection()[maxComp], 0);
			}
		}else{
			// if we have 2 or more non-zero components, use algorithm
			// find first two orthogonal vectors with formula
			if (maxComp == 0){
				o[0] = new Vector(-2*v.getY()*v.getZ()/v.getX(), v.getZ(), v.getY());
				o[1] = o[0].multiply(-1);
			}else if (maxComp == 1){
				o[0] = new Vector(v.getZ(), -2*v.getX()*v.getZ()/v.getY(), v.getX());
				o[1] = o[0].multiply(-1);
			}else if (maxComp == 2){
				o[0] = new Vector(v.getY(), v.getX(), -2*v.getX()*v.getY()/v.getZ());
				o[1] = o[0].multiply(-1);
			}
			
			// find second orthogonal vector with cross product
			o[2] = new Vector(	o[0].getY() * v.getZ() - o[0].getZ() * v.getY(),
								o[0].getZ() * v.getX() - o[0].getX() * v.getZ(),
								o[0].getX() * v.getY() - o[0].getY() * v.getX());
			o[3] = o[2].multiply(-1);
		}
		
		// ensure all vectors have the appropriate length
		for (int i = 0; i < 4; i++)
			o[i] = o[i].multiply(v.getLength() / o[i].getLength());

		// find "starting point" of arrowheads
		final float mx = endx - 3 * v.getX();
		final float my = endy - 3 * v.getY();
		final float mz = endz - 3 * v.getZ();
		
		vertices[ 0] = startx;
		vertices[ 1] = starty;
		vertices[ 2] = startz;
		vertices[ 3] = endx;
		vertices[ 4] = endy;
		vertices[ 5] = endz;

		for (int i = 0; i < 4; i++){
			vertices[6 * (i + 1) + 0] = endx;
			vertices[6 * (i + 1) + 1] = endy;
			vertices[6 * (i + 1) + 2] = endz;
			vertices[6 * (i + 1) + 3] = mx + o[i].getX();
			vertices[6 * (i + 1) + 4] = my + o[i].getY();
			vertices[6 * (i + 1) + 5] = mz + o[i].getZ();
		}
		
		return vertices;
	}

	private float[] getColorData(final float[] color) {
		final float[] result = new float[10 * 3 * color.length];

		for (int i = 0; i < result.length / color.length; i++)
			System.arraycopy(color, 0, result, i * color.length, color.length);
		return result;
	}

	@Override
	protected String getShapeTag() {
		return TAG;
	}
}
