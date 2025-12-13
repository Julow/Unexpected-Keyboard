package juloo.keyboard2.dict;

import juloo.keyboard2.R;

public enum SupportedDictionaries
{
  /** Enumeration of the supported dictionaries. */

  AR("ar", R.string.dict_name_ar, 455658),
  AS("as", R.string.dict_name_as, 307451),
  BE("be", R.string.dict_name_be, 1745968),
  BG("bg", R.string.dict_name_bg, 347735),
  BN("bn", R.string.dict_name_bn, 301586),
  BS("bs", R.string.dict_name_bs, 481783),
  CA("ca", R.string.dict_name_ca, 335099),
  CS("cs", R.string.dict_name_cs, 657214),
  DA("da", R.string.dict_name_da, 933060),
  DE("de", R.string.dict_name_de, 1071580),
  DE_CH("de_CH", R.string.dict_name_de_ch, 1075177),
  EL("el", R.string.dict_name_el, 964195),
  EN_AU("en_AU", R.string.dict_name_en_au, 645068),
  EN_GB("en_GB", R.string.dict_name_en_gb, 644719),
  EN_US("en_US", R.string.dict_name_en_us, 650605),
  ES("es", R.string.dict_name_es, 635119),
  EU("eu", R.string.dict_name_eu, 359849),
  FI("fi", R.string.dict_name_fi, 1003794),
  FR("fr", R.string.dict_name_fr, 871636),
  GL("gl", R.string.dict_name_gl, 240926),
  GU("gu", R.string.dict_name_gu, 279333),
  HI("hi", R.string.dict_name_hi, 301928),
  HR("hr", R.string.dict_name_hr, 727487),
  HU("hu", R.string.dict_name_hu, 331818),
  HY("hy", R.string.dict_name_hy, 948362),
  IT("it", R.string.dict_name_it, 696974),
  IW("iw", R.string.dict_name_iw, 352933),
  KA("ka", R.string.dict_name_ka, 488139),
  KM("km", R.string.dict_name_km, 263411),
  KN("kn", R.string.dict_name_kn, 286588),
  LB("lb", R.string.dict_name_lb, 331897),
  LT("lt", R.string.dict_name_lt, 736799),
  LV("lv", R.string.dict_name_lv, 854603),
  MAI("mai", R.string.dict_name_mai, 333644),
  ML("ml", R.string.dict_name_ml, 354875),
  MR("mr", R.string.dict_name_mr, 301259),
  NB("nb", R.string.dict_name_nb, 776481),
  NL("nl", R.string.dict_name_nl, 926933),
  OR("or", R.string.dict_name_or, 357107),
  PA("pa", R.string.dict_name_pa, 107120),
  PL("pl", R.string.dict_name_pl, 740895),
  PT_BR("pt_BR", R.string.dict_name_pt_br, 705536),
  PT_PT("pt_PT", R.string.dict_name_pt_pt, 996904),
  RO("ro", R.string.dict_name_ro, 1421447),
  RU("ru", R.string.dict_name_ru, 907625),
  SA("sa", R.string.dict_name_sa, 342293),
  SAT("sat", R.string.dict_name_sat, 322250),
  SD("sd", R.string.dict_name_sd, 256078),
  SL("sl", R.string.dict_name_sl, 228729),
  SR("sr", R.string.dict_name_sr, 740293),
  SV("sv", R.string.dict_name_sv, 916845),
  TA("ta", R.string.dict_name_ta, 297338),
  TE("te", R.string.dict_name_te, 304295),
  TR("tr", R.string.dict_name_tr, 690598),
  UK("uk", R.string.dict_name_uk, 1179318),
  UR("ur", R.string.dict_name_ur, 265324),
  ZGH("zgh", R.string.dict_name_zgh, 628168);

  /** Associated information. */

  public final String locale; /** Locale that matches this dictionary. */
  public final int name_resource; /** Display name. */
  public final int size; /** Size in bytes of the dictionary file. */

  SupportedDictionaries(String l, int r, int s)
  { locale = l; name_resource = r; size = s; }

  /** Name used in preferences, URLs and file names. */
  public String internal_name() { return locale; }
}
