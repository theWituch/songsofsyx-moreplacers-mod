package settlement.job;

import static settlement.main.SETT.JOBS;
import static settlement.main.SETT.ROOMS;
import static settlement.main.SETT.TERRAIN;
import static settlement.main.SETT.TWIDTH;

import game.GAME;
import game.GameDisposable;
import game.audio.SoundRace;
import game.faction.FResources.RTYPE;
import init.constant.C;
import init.race.RACES;
import init.race.Race;
import init.resources.RESOURCE;
import init.resources.STOCKPILE.StockpileImp;
import init.sprite.SPRITES;
import init.structure.STRUCTURES;
import init.structure.Structure;
import init.type.BUILDING_PREFS;
import settlement.entity.humanoid.Humanoid;
import settlement.main.SETT;
import settlement.tilemap.terrain.TBuilding;
import settlement.tilemap.terrain.Terrain.TerrainTile;
import snake2d.SPRITE_RENDERER;
import snake2d.util.color.ColorImp;
import snake2d.util.datatypes.AREA;
import snake2d.util.datatypes.DIR;
import snake2d.util.gui.GUI_BOX;
import snake2d.util.gui.GuiSection;
import snake2d.util.gui.clickable.CLICKABLE;
import snake2d.util.gui.renderable.RENDEROBJ;
import snake2d.util.misc.ACTION;
import snake2d.util.misc.CLAMP;
import snake2d.util.sets.ArrayList;
import snake2d.util.sets.LIST;
import snake2d.util.sets.LISTE;
import snake2d.util.sets.LinkedList;
import snake2d.util.sprite.SPRITE;
import snake2d.util.sprite.text.Str;
import util.colors.GCOLOR;
import util.gui.misc.GBox;
import util.gui.misc.GButt;
import util.gui.panel.GPanel;
import util.info.GFORMAT;
import util.text.D;
import view.main.VIEW;
import view.subview.GameWindow;
import view.tool.PLACABLE;
import view.tool.PLACER_TYPE;
import view.tool.PlacableMessages;
import view.tool.PlacableMulti;
import view.tool.ToolConfig;

public class JobBuildStructure {

	public final Structure building;
	public final TBuilding terrain;
	public final Job wall;
	public final Job ceiling;
	public final PlacableMulti combo;
	public final PlacableMulti convert;

	private static CharSequence ¤¤WallD = "¤Walls can be used to fence off areas.";
	private static CharSequence ¤¤CeilingD = "¤Ceilings can house rooms inside them and protects subjects from the elements.";
	private static CharSequence ¤¤Structure = "¤{0} Room.";
	private static CharSequence ¤¤StructureD = "A combination tool that makes ceilings surrounded by walls of a chosen material.";

	private static CharSequence ¤¤Convert = "Convert";
	private static CharSequence ¤¤ConvertD = "Convert existing structures into this type.";
	private static CharSequence ¤¤ConvertR = "Not enough resources in warehouses.";

	private static CharSequence ¤¤SameProblem = "Must be placed on a structure of a different type.";

	private static CharSequence ¤¤constructions = "Construction Seconds";

	static {
		D.ts(JobBuildStructure.class);
	}

	private JobBuildStructure(Structure building) {
		this.building = building;
		terrain = SETT.TERRAIN().BUILDINGS.get(building);
		this.wall = new Wall();
		this.ceiling = new Roof();
		this.combo = new Combo();
		this.convert = new Convert();

	}

	static LIST<JobBuildStructure> make(){
		ArrayList<JobBuildStructure> all = new ArrayList<>(STRUCTURES.all().size());
		for (Structure s : STRUCTURES.all()) {
			all.add(new JobBuildStructure(s));
		}
		return all;
	}

	private final class Wall extends JobBuild {

		Wall() {
			super("WALL_" + building.key, building.resource, building.resAmount+1, true, building.nameWall,
					¤¤WallD, terrain.wall.getIcon());
		}

		@Override
		void renderAbove(SPRITE_RENDERER r, int x, int y, int mask, int tx, int ty) {
			for (DIR d : DIR.ORTHO) {
				Job j = JOBS().getter.get(tx, ty, d);
				if (j instanceof Wall || terrain.wall.is(tx, ty))
					mask |= d.mask();
			}
			SPRITES.cons().BIG.dashedThick.render(r, mask, x, y);
		}

		@Override
		protected CharSequence problem(int tx, int ty, boolean overwrite) {
			if (ROOMS().map.is(tx, ty))
				return PlacableMessages.¤¤ROOM_BLOCK;

			if (TERRAIN().MOUNTAIN.isMountain(tx, ty)) {
				return PlacableMessages.¤¤MOUNTAIN_NOT;
			}
			TerrainTile t = TERRAIN().get(tx, ty);
			if (t == SETT.TERRAIN().WATER.DEEP) {
				return PlacableMessages.¤¤MISC;
			}
			if (t.clearing().needs() && !t.clearing().can())
				return PlacableMessages.¤¤MISC;
			if (JOBS().getter.get(tx, ty) == this)
				return PLACABLE.E;
			if (t == terrain.wall)
				return PLACABLE.E;
			if (!overwrite) {
				if (JOBS().getter.is(tx, ty)) {
					return PlacableMessages.¤¤JOB_BLOCK;
				}
			}
			return null;
		}

		@Override
		boolean terrainNeedsClear(int tx, int ty) {
			if (terrain.roof.is(tx, ty))
				return false;
			return super.terrainNeedsClear(tx, ty);
		}

		@Override
		boolean resNeeds(int tx, int ty) {
			if (terrain.roof.is(tx, ty))
				return res != null && JOBS().progress.get(tx + ty * TWIDTH) == 0;
			return super.resNeeds(tx, ty);
		}

		@Override
		protected double constructionTime(Humanoid skill) {
			return CLAMP.d(building.constructTime*50, 1, 1500);
		}

		@Override
		protected SoundRace constructSound() {
			return terrain.sound;
		}

		@Override
		protected boolean construct(int tx, int ty) {
			if (building.resource != null)
				GAME.player().res().inc(building.resource,  RTYPE.CONSTRUCTION, -(building.resAmount+1));
			terrain.wall.placeFixed(tx, ty);
			return false;
		}

		@Override
		public boolean becomesSolid() {
			return true;
		}

		@Override
		public boolean isConstruction() {
			return true;
		}

		@Override
		public TerrainTile becomes(int tx, int ty) {
			return terrain.wall;
		}

		@Override
		protected void extraHovInfo(GBox box) {
			box.textLL(¤¤constructions);
			box.add(GFORMAT.i(box.text(), (int)constructionTime(null)));
		}

		@Override
		public ToolConfig config() {
			return con(JobBuildStructure.this, this.placer());
		}

	}

	private final class Roof extends JobBuild {

		Roof() {
			super("CEILING_" + building.key, building.resource, building.resAmount, false, building.nameCeiling,
					¤¤CeilingD, terrain.roof.getIcon());
		}

		@Override
		void renderAbove(SPRITE_RENDERER r, int x, int y, int mask, int tx, int ty) {
			for (DIR d : DIR.ORTHO) {
				Job j = JOBS().getter.get(tx, ty, d);
				if (j instanceof Wall || j instanceof Roof || terrain.roof.is(tx, ty))
					mask |= d.mask();
			}
			SPRITES.cons().BIG.dashed.render(r, mask, x, y);

		}

		@Override
		protected CharSequence problem(int tx, int ty, boolean overwrite) {
			if (SETT.TERRAIN().get(tx, ty) == SETT.TERRAIN().WATER.DEEP) {
				return PlacableMessages.¤¤MISC;
			}
			if (terrain.wall.is(tx, ty) && overwrite)
				return null;
			return super.problem(tx, ty, overwrite);
		}

		@Override
		protected double constructionTime(Humanoid skill) {
			return 1 + building.constructTime*140;
		}

		@Override
		protected SoundRace constructSound() {
			return terrain.sound;
		}

		@Override
		boolean terrainNeedsClear(int tx, int ty) {
			if (terrain.wall.is(tx, ty))
				return false;
			return super.terrainNeedsClear(tx, ty);
		}

		@Override
		boolean resNeeds(int tx, int ty) {
			if (terrain.wall.is(tx, ty))
				return false;
			return super.resNeeds(tx, ty);
		}

		@Override
		protected boolean construct(int tx, int ty) {
			if (building.resource != null)
				GAME.player().res().inc(building.resource, RTYPE.CONSTRUCTION, -building.resAmount);
			terrain.roof.placeFixed(tx, ty);
			return false;
		}

		@Override
		public boolean isConstruction() {
			return true;
		}

		@Override
		public TerrainTile becomes(int tx, int ty) {
			return terrain.roof;
		}

		@Override
		protected void extraHovInfo(GBox box) {
			box.textLL(¤¤constructions);
			box.add(GFORMAT.i(box.text(), (int)constructionTime(null)));
		}

		@Override
		public ToolConfig config() {
			return con(JobBuildStructure.this, this.placer());
		}

	}

	private final class Combo extends PlacableMulti {

		public Combo() {
			super(new Str(¤¤Structure).insert(0, building.name), ¤¤StructureD, terrain.iconCombo);
		}

		@Override
		public CharSequence isPlacable(int tx, int ty, AREA a, PLACER_TYPE t) {
			if (isWall(tx, ty, a))
				return wall.placer().isPlacable(tx,  ty, a, t);
			return ceiling.placer().isPlacable(tx,  ty, a, t);
		}

		@Override
		public void place(int tx, int ty, AREA a, PLACER_TYPE t) {
			if (isWall(tx, ty, a))
				wall.placer().place(tx, ty, a, t);
			else
				ceiling.placer().place(tx, ty, a, t);
		}

		@Override
		public void renderPlaceHolder(SPRITE_RENDERER r, int mask, int x, int y, int tx, int ty, AREA a,
									  PLACER_TYPE t, boolean isPlacable, boolean areaIsPlacable) {
			if (isWall(tx, ty, a))
				wall.renderAbove(r, x, y, mask, tx, ty);
			else
				ceiling.renderAbove(r, x, y, mask, tx, ty);
		}

		private boolean isWall(int tx, int ty, AREA a) {
			for (DIR d : DIR.ALL) {
				if (!a.is(tx, ty, d)) {
					int y1 = a.body().y1();
					int y2 = a.body().y2();
					int x1 = a.body().x1();
					int x2 = a.body().x2();
					if ((a.body().height() & 1) == 1) {
						y1 = a.body().cY();
						y2 = a.body().cY();
					}else {
						y1 = a.body().cY()-1;
						y2 = a.body().cY();
					}

					if ((a.body().width() & 1) == 1) {
						x1 = a.body().cX();
						x2 = a.body().cX();
					}else {
						x1 = a.body().cX()-1;
						x2 = a.body().cX();
					}

					return (tx < x1 || tx > x2) && (ty < y1 || ty > y2);
				}
			}
			return false;
		}

		@Override
		public boolean canBePlacedAs(PLACER_TYPE t) {
			return t != PLACER_TYPE.LINE;
		}

		@Override
		public PLACABLE getUndo() {
			return wall.placer().getUndo();
		}
	}

	private final class Convert extends PlacableMulti {

		public Convert() {
			super(¤¤Convert, ¤¤ConvertD, new SPRITE.Twin(terrain.wall.getIcon(), SPRITES.icons().m.arrow_right));
		}


		boolean count = true;
		int res = 0;
		int allocated = 0;

		@Override
		public void updateRegardless(GameWindow window, AREA selected) {
			count = true;
			res = 0;
			allocated = 0;
		}

		@Override
		public void finishChecking(AREA placedArea) {
			count = false;
			super.finishChecking(placedArea);
		}

		@Override
		public CharSequence isPlacable(int tx, int ty, AREA a, PLACER_TYPE t) {

			TerrainTile te = SETT.TERRAIN().get(tx, ty);
			if (te == null || !(te instanceof TBuilding.BuildingComponent))
				return ¤¤SameProblem;
			if (terrain.isser.is(tx, ty))
				return ¤¤SameProblem;

			if (count) {
				if (te instanceof TBuilding.Wall && wall.res() != null)
					res += wall.resAmount();
				else if ((te instanceof TBuilding.Ceiling || te instanceof TBuilding.Ceiling.Opening) && ceiling.res() != null)
					res += ceiling.resAmount();

				if (res >= SETT.ROOMS().STOCKPILE.tally().amountReservable.get(wall.res())) {
					return ¤¤ConvertR;
				}
			}


			return null;
		}

		@Override
		public void finishPlacing(AREA placedArea) {
			if (res > 1) {
				StockpileImp stock = new StockpileImp();
				stock.set(wall.res(), res-1);
				res = 0;
				allocated = 0;
				RESOURCE.remove(stock, RTYPE.CONSTRUCTION);

			}
		}

		@Override
		public void place(int tx, int ty, AREA a, PLACER_TYPE t) {
			if (terrain.isser.is(tx, ty))
				return;
			TerrainTile te = SETT.TERRAIN().get(tx, ty);
			if (te instanceof TBuilding.Wall && allocated + wall.resAmount() < SETT.ROOMS().STOCKPILE.tally().amountReservable.get(wall.res())) {
				terrain.wall.placeFixed(tx, ty);
				allocated += wall.resAmount();
			}
			else if ((te instanceof TBuilding.Ceiling || te instanceof TBuilding.Ceiling.Opening) && allocated + ceiling.resAmount() < SETT.ROOMS().STOCKPILE.tally().amountReservable.get(wall.res())) {
				terrain.roof.placeFixed(tx, ty);
				allocated += ceiling.resAmount();
			}
		}

		@Override
		public PLACABLE getUndo() {
			return JOBS().tool_clear;
		}



		@Override
		public void placeInfo(GBox b, int oktiles, AREA a) {
			if (wall.res() != null) {
				b.add(wall.res().icon());
				b.add(GFORMAT.iofk(b.text(), res, SETT.ROOMS().STOCKPILE.tally().amountReservable.get(wall.res())));
			}
			super.placeInfo(b, oktiles, a);
		}
	}


	private static Con pla = null;
	static {
		new GameDisposable() {

			@Override
			protected void dispose() {
				pla = null;
			}
		};
	}

	private static ToolConfig con(JobBuildStructure struc, PlacableMulti job) {

		if (pla == null)
			pla = new Con();
		return pla.get(struc, job);

	}

	public static Job getPlacable() {
		if (pla == null)
			pla = new Con();
		if (pla.struc.wall == null || pla.struc.wall.lockText() != null) {
			for (JobBuildStructure j : SETT.JOBS().build_structure) {
				if (j.wall.lockText() == null)
					return j.wall;
			}
		}
		return pla.struc.wall;
	}

	private static class Con implements ToolConfig {

		private final LinkedList<CLICKABLE> butts = new LinkedList<>();
		private JobBuildStructure struc;
		private int type = 0;
		PlacableMulti job;
		private final GuiSection section = new GuiSection();
		private GuiSection full = new GuiSection();
		private final GPanel panel = new GPanel();

		ACTION exit = new ACTION() {

			@Override
			public void exe() {
				VIEW.s().tools.placer.deactivate();
			}
		};

		public ToolConfig get(JobBuildStructure struc, PlacableMulti job) {
			if (job == struc.combo)
				type = 0;
			else if (job == struc.wall.placer()) {
				type = 1;
			}else if (job == struc.ceiling.placer())
				type = 2;
			else
				type = 3;
			this.struc = struc;
			this.job = job;
			return this;
		}

		Con(){

			butts.add(new GButt.ButtPanel(SPRITES.icons().m.wallceiling) {

				@Override
				protected void clickA() {
					job = struc.combo;
					type = 0;
					VIEW.s().tools.place(job, Con.this);
				};

				@Override
				public void hoverInfoGet(GUI_BOX text) {
					struc.combo.hoverDesc((GBox) text);
				}

				@Override
				protected void renAction() {
					selectedSet(type == 0);
				}

			});

			butts.add(new GButt.ButtPanel(SPRITES.icons().m.wall) {

				@Override
				protected void clickA() {
					job = struc.wall.placer();
					type = 1;
					VIEW.s().tools.place(job, Con.this);
				};

				@Override
				public void hoverInfoGet(GUI_BOX text) {
					struc.wall.placer().hoverDesc((GBox) text);
				}

				@Override
				protected void renAction() {
					selectedSet(type == 1);
				}

			});

			butts.add(new GButt.ButtPanel(SPRITES.icons().m.wall_opening) {

				@Override
				protected void clickA() {
					job = struc.ceiling.placer();
					type = 2;
					VIEW.s().tools.place(job, Con.this);
				};

				@Override
				public void hoverInfoGet(GUI_BOX text) {
					struc.ceiling.placer().hoverDesc((GBox) text);
				}

				@Override
				protected void renAction() {
					selectedSet(type == 2);
				}

			});

			butts.add(new GButt.ButtPanel(SPRITES.icons().m.arrow_right) {

				@Override
				protected void clickA() {
					job = struc.convert;
					type = 3;
					VIEW.s().tools.place(job, Con.this);
				};

				@Override
				public void hoverInfoGet(GUI_BOX text) {
					struc.convert.hoverDesc((GBox) text);
				}

				@Override
				protected void renAction() {
					selectedSet(type == 3);
				}

			});

			for (JobBuildStructure j : SETT.JOBS().build_structure) {


				GButt.ButtPanel b = new GButt.ButtPanel(j.wall.placer().getIcon()) {

					@Override
					protected void clickA() {
						setStruc(j);
						VIEW.s().tools.place(job, Con.this);
					};

					@Override
					public void hoverInfoGet(GUI_BOX text) {
						text.title(j.building.name);
						text.text(j.building.desc);
						GBox b = (GBox) text;
						b.NL();
						if (j.building.resource != null)
							b.setResource(j.building.resource, j.building.resAmount+1);

						b.NL(8);

						for (Race r : RACES.all()) {
							double d = r.pref().structure(BUILDING_PREFS.get(j.building));
							int k = 1 + (int) (5*d);
							if ((r.index & 0b0011) == 0)
								b.NL();
							b.tab((r.index&0b011)*3);
							b.add(r.appearance().icon);
							ColorImp.TMP.interpolate(GCOLOR.UI().BAD.hovered, GCOLOR.UI().GOOD.hovered, d);
							for (int i = 0; i < k; i++) {
								b.add(SPRITES.icons().s.heart, ColorImp.TMP);
								b.rewind(8);
							}
							b.space();

						}

					}

					@Override
					protected void renAction() {
						selectedSet(struc == j);
					}

				};

				section.addRightC(0, b);
				if (j.wall.lockText() == null)
					struc = j;
			}
		}


		void setStruc(JobBuildStructure struc) {
			this.struc = struc;
			switch(type) {
				case 0: job = struc.combo; break;
				case 1: job = struc.wall.placer(); break;
				case 2: job = struc.ceiling.placer(); break;
				case 3: job = struc.convert; break;
			}
		}



		@Override
		public void addUI(LISTE<RENDEROBJ> uis) {
			full.clear();

			VIEW.s().tools.placer.stealButtons(full);
			for (CLICKABLE c : butts) {
				full.addRightC(0, c);
			}
			if (job.getAdditionalButt() != null)
				for (CLICKABLE p : job.getAdditionalButt())
					full.addRightC(0, p);
			full.body().centerX(C.DIM());
			full.addRelBody(C.SG*8, DIR.N, section);

			panel.setButt();
			panel.inner().set(full);
			panel.clickActionSet(exit);
			full.add(panel);
			full.moveLastToBack();
			full.body().moveY1(90);
			uis.add(full);
		}
	}

}
