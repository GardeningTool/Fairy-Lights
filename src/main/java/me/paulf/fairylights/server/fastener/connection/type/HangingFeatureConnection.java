package me.paulf.fairylights.server.fastener.connection.type;

import com.google.common.base.MoreObjects;
import me.paulf.fairylights.server.fastener.Fastener;
import me.paulf.fairylights.server.fastener.connection.Catenary;
import me.paulf.fairylights.server.fastener.connection.Feature;
import me.paulf.fairylights.server.fastener.connection.FeatureType;
import me.paulf.fairylights.server.fastener.connection.Segment;
import me.paulf.fairylights.server.fastener.connection.collision.Collidable;
import me.paulf.fairylights.server.fastener.connection.collision.FeatureCollisionTree;
import me.paulf.fairylights.server.fastener.connection.type.HangingFeatureConnection.HangingFeature;
import me.paulf.fairylights.util.AABBBuilder;
import me.paulf.fairylights.util.Mth;
import me.paulf.fairylights.util.matrix.MatrixStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class HangingFeatureConnection<F extends HangingFeature> extends Connection {
	protected static final FeatureType FEATURE = FeatureType.create("feature");

	protected F[] features = createFeatures(0);

	@Nullable
	protected F[] prevFeatures;

	public HangingFeatureConnection(World world, Fastener<?> fastener, UUID uuid, Fastener<?> destination, boolean isOrigin, CompoundNBT compound) {
		super(world, fastener, uuid, destination, isOrigin, compound);
	}

	public HangingFeatureConnection(World world, Fastener<?> fastener, UUID uuid) {
		super(world, fastener, uuid);
	}

	public final F[] getFeatures() {
		return features;
	}

	public final F[] getPrevFeatures() {
		return MoreObjects.firstNonNull(prevFeatures, features);
	}

	@Override
	protected void updatePrev() {
		prevFeatures = features;
	}

	@Override
	protected void onCalculateCatenary() {
		updateFeatures();
	}

	protected void updateFeatures() {
		Catenary catenary = getCatenary();
		float spacing = getFeatureSpacing();
		float totalLength = catenary.getLength();
		if (totalLength > 2.0F * Connection.MAX_LENGTH * 16.0F) {
			prevFeatures = features;
			onBeforeUpdateFeatures(0);
			features = createFeatures(0);
			onAfterUpdateFeatures(0);
			return;
		}
		/*
		 * Distance starts at the value that will center the lights on the cord
		 * Simplified version of t / 2 - ((int) (t / s) - 1) * s / 2
		 */
		float distance = (totalLength % spacing + spacing) / 2;
		Segment[] segments = catenary.getSegments();
		prevFeatures = features;
		// Can't use array since there is an elusive edge case were the capacity is off by one
		List<F> features = new ArrayList<>((int) (totalLength / spacing));
		onBeforeUpdateFeatures(features.size());
		boolean hasPrevLights = prevFeatures != null;
		int index = 0;
		for (int i = 0; i < segments.length; i++) {
			Segment segment = segments[i];
			double length = segment.getLength();
			while (distance < length) {
				F feature = createFeature(index, segment.pointAt(distance / length), segment.getRotation());
				if (hasPrevLights && index < prevFeatures.length) {
					feature.inherit(prevFeatures[index]);
				}
				features.add(feature);
				distance += spacing;
				index++;
			}
			distance -= length;
		}
		this.features = features.toArray(createFeatures(features.size()));
		onAfterUpdateFeatures(index);
	}

	protected abstract F[] createFeatures(int length);

	protected abstract F createFeature(int index, Vec3d point, Vec3d rotation);

	protected abstract float getFeatureSpacing();

	protected void onBeforeUpdateFeatures(int size) {}

	protected void onAfterUpdateFeatures(int size) {}

	@Override
	public void addCollision(List<Collidable> collision, Vec3d origin) {
		super.addCollision(collision, origin);
		if (features.length > 0) {
			MatrixStack matrix = new MatrixStack();
			collision.add(FeatureCollisionTree.build(FEATURE, features, f -> {
				Vec3d pos = f.getPoint();
				double x = origin.x + pos.x / 16;
				double y = origin.y + pos.y / 16;
				double z = origin.z + pos.z / 16;
				double w = f.getWidth() / 2;
				double h = f.getHeight();
				matrix.push();
				Vec3d rot = f.getRotation();
				if (f.parallelsCord()) {
					matrix.rotate((float) rot.x, 0, 1, 0);
					matrix.rotate((float) rot.y, 1, 0, 0);
				}
				matrix.translate(0, 0.025F, 0);
				AABBBuilder bounds = new AABBBuilder();
				Vec3d[] verts = {
					new Vec3d(-w, -h, -w),
					new Vec3d(w, -h, -w),
					new Vec3d(w, -h, w),
					new Vec3d(-w, -h, w),
					new Vec3d(-w, 0, -w),
					new Vec3d(w, 0, -w),
					new Vec3d(w, 0, w),
					new Vec3d(-w, 0, w)
				};
				for (Vec3d vert : verts) {
					bounds.include(matrix.transform(vert));
				}
				matrix.pop();
				return bounds.add(x, y, z).build();
			}));
		}
	}

	public static abstract class HangingFeature<F extends HangingFeature<F>> implements Feature {
		protected final int index;

		protected final Vec3d point;

		protected Vec3d rotation;

		protected Vec3d prevRotation;

		public HangingFeature(int index, Vec3d point, Vec3d rotation) {
			this.index = index;
			this.point = point;
			this.prevRotation = this.rotation = rotation;
		}

		@Override
		public final int getId() {
			return index;
		}

		public final Vec3d getPoint() {
			return point;
		}

		public final Vec3d getRotation() {
			return rotation;
		}

		public final Vec3d getRotation(float t) {
			return Mth.lerpAngles(prevRotation, rotation, t);
		}

		public final Vec3d getAbsolutePoint(Fastener<?> fastener) {
			return point.scale(0.0625F).add(fastener.getConnectionPoint());
		}

		public void inherit(F feature) {
			prevRotation = new Vec3d(prevRotation.x, prevRotation.y, feature.prevRotation.z);
			rotation = new Vec3d(rotation.x, rotation.y, feature.rotation.z);
		}

		public abstract double getWidth();

		public abstract double getHeight();

		public abstract boolean parallelsCord();
	}
}
