package me.paulf.fairylights.client.model.connection;

import com.mojang.blaze3d.platform.GlStateManager;
import me.paulf.fairylights.client.model.AdvancedRendererModel;
import me.paulf.fairylights.client.model.RotationOrder;
import me.paulf.fairylights.server.fastener.Fastener;
import me.paulf.fairylights.server.fastener.connection.type.pennant.PennantBuntingConnection;
import me.paulf.fairylights.server.fastener.connection.type.pennant.Pennant;
import me.paulf.fairylights.util.Mth;
import me.paulf.fairylights.util.styledstring.StyledString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PennantBuntingConnectionModel extends ConnectionModel<PennantBuntingConnection> {
	private AdvancedRendererModel cordModel;

	private AdvancedRendererModel[] pennantModels = { createPennant(63), createPennant(72), createPennant(81) };

	public PennantBuntingConnectionModel() {
		cordModel = new AdvancedRendererModel(this, 0, 17);
		cordModel.addBox(-0.5F, -0.5F, 0, 1, 1, 1);
		cordModel.scaleX = 1.5F;
		cordModel.scaleY = 1.5F;
	}

	private AdvancedRendererModel createPennant(int u) {
		AdvancedRendererModel pennant = new AdvancedRendererModel(this, u, 16);
		pennant.add3DTexture(-4.5F, -10, 0.5F, 9, 10);
		pennant.setRotationOrder(RotationOrder.YXZ);
		pennant.secondaryRotateAngleY = Mth.HALF_PI;
		return pennant;
	}

	@Override
	public boolean hasTexturedRender() {
		return true;
	}

	@Override
	public void render(Fastener<?> fastener, PennantBuntingConnection bunting, World world, int skylight, int moonlight, float delta) {
		super.render(fastener, bunting, world, skylight, moonlight, delta);
		Pennant[] pennants = bunting.getFeatures();
		Pennant[] prevPennants = bunting.getPrevFeatures();
		GlStateManager.disableCull();
		for (int i = 0, count = Math.min(pennants.length, prevPennants.length); i < count; i++) {
			AdvancedRendererModel model = preparePennantModel(pennants, prevPennants, i, delta);
			int rgb = pennants[i].getColor();
			GlStateManager.color3f(((rgb >> 16) & 0xFF) / 255F, ((rgb >> 8) & 0xFF) / 255F, (rgb & 0xFF) / 255F);
			model.render(0.0625F);
		}
		GlStateManager.enableCull();
		GlStateManager.color3f(1, 1, 1);
	}

	@Override
	public void renderTexturePass(Fastener<?> fastener, PennantBuntingConnection bunting, World world, int skylight, int moonlight, float delta) {
		Pennant[] pennants = bunting.getFeatures();
		Pennant[] prevPennants = bunting.getPrevFeatures();
		StyledString text = bunting.getText();
		int pennantCount = Math.min(pennants.length, prevPennants.length);
		int offset;
		if (text.length() > pennantCount) {
			int over = text.length() - pennantCount;
			int lower = over / 2;
			text = text.substring(lower, text.length() - over + lower);
			offset = 0;
		} else {
			offset = pennantCount / 2 - text.length() / 2;
		}
		FontRenderer font = Minecraft.getInstance().fontRenderer;
		for (int i = 0; i < text.length(); i++) {
			int pennantIndex = i + offset;
			StyledString chrA = text.substring(i, i + 1);
			StyledString chrB = text.substring(text.length() - i - 1, text.length() - i);
			String charAStr = chrA.toString();
			String charBStr = chrB.toString();
			AdvancedRendererModel model = preparePennantModel(pennants, prevPennants, pennantIndex, delta);
			GlStateManager.pushMatrix();
			model.postRender(0.0625F);
			float s = 0.03075F;
			GlStateManager.pushMatrix();
			GlStateManager.translatef(0, -0.25F, -0.04F);
			GlStateManager.scalef(-s, -s, s);
			GlStateManager.translatef(-font.getStringWidth(charAStr) / 2F + 0.5F, -4, 0);
			GlStateManager.normal3f(0, 0, 1);
			font.drawString(charAStr, 0, 0, 0xFFFFFFFF);
			GlStateManager.popMatrix();
			GlStateManager.translatef(0, -0.25F, 0.04F);
			GlStateManager.scalef(s, -s, s);
			GlStateManager.translatef(-font.getStringWidth(charBStr) / 2F + 0.5F, -4, 0);
			GlStateManager.normal3f(0, 0, -1);
			font.drawString(charBStr, 0, 0, 0xFFFFFFFF);
			GlStateManager.popMatrix();
		}
	}

	private AdvancedRendererModel preparePennantModel(Pennant[] pennants, Pennant[] prevPennants, int index, float delta) {
		Pennant pennant = pennants[index];
		Vec3d point = Mth.lerp(prevPennants[index].getPoint(), pennant.getPoint(), delta);
		Vec3d rotation = Mth.lerpAngles(prevPennants[index].getRotation(), pennant.getRotation(), delta);
		AdvancedRendererModel model = pennantModels[index % pennantModels.length];
		model.setRotationPoint(point.x, point.y, point.z);
		model.setRotationAngles(rotation.y, rotation.x, rotation.z);
		return model;
	}

	@Override
	protected void renderSegment(PennantBuntingConnection connection, int index, double angleX, double angleY, double length, double x, double y, double z, float delta) {
		cordModel.rotateAngleX = (float) angleX;
		cordModel.rotateAngleY = (float) angleY;
		cordModel.scaleZ = (float) length;
		cordModel.setRotationPoint(x, y, z);
		cordModel.render(0.0625F);
	}
}
