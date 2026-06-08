package QualityControlDashboard;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

// ============================================================
// SISTEM MONITORING KUALITAS PRODUK (QUALITY CONTROL DASHBOARD)
// Materi: Week 1–11 Dasar Pemrograman Java
// + LOGIN / REGISTRASI + SORTIR TANGGAL + FILTER MIN-MAX
// Storage: File CSV (data/akun.csv, data/produk.csv, data/hasil_inspeksi.csv)
// ============================================================

// ============================================================
// PENYIMPANAN DATA CSV (pengganti MySQL)
// Semua data disimpan di folder "data/" relatif terhadap .jar
// ============================================================
class CsvStorage {
    // Folder data diletakkan di samping file .class / .jar
    static final String DIR = "data" + File.separator;

    static final String FILE_AKUN     = DIR + "akun.csv";
    static final String FILE_PRODUK   = DIR + "produk.csv";
    static final String FILE_INSPEKSI = DIR + "hasil_inspeksi.csv";

    /** Baca semua baris CSV (skip header), kembalikan List<String[]> */
    static List<String[]> baca(String filePath) {
        List<String[]> rows = new ArrayList<>();
        File f = new File(filePath);
        if (!f.exists()) return rows;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) rows.add(parseCsvLine(line));
            }
        } catch (IOException e) {
            System.err.println("Gagal baca " + filePath + ": " + e.getMessage());
        }
        return rows;
    }

    /** Tulis ulang seluruh file CSV dengan header + rows */
    static void tulis(String filePath, String header, List<String[]> rows) {
        new File(DIR).mkdirs();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(filePath, false), StandardCharsets.UTF_8))) {
            pw.println(header);
            for (String[] r : rows) pw.println(gabungCsv(r));
        } catch (IOException e) {
            System.err.println("Gagal tulis " + filePath + ": " + e.getMessage());
        }
    }

    /** Tambah satu baris ke akhir file CSV (append) */
    static void tambahBaris(String filePath, String header, String[] row) {
        new File(DIR).mkdirs();
        File f = new File(filePath);
        boolean kosong = !f.exists() || f.length() == 0;
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(filePath, true), StandardCharsets.UTF_8))) {
            if (kosong) pw.println(header);
            pw.println(gabungCsv(row));
        } catch (IOException e) {
            System.err.println("Gagal append " + filePath + ": " + e.getMessage());
        }
    }

    /** Parse satu baris CSV dengan dukungan field yang mengandung koma dalam tanda kutip */
    static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (c == ',' && !inQuote) {
                result.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString());
        return result.toArray(new String[0]);
    }

    /** Gabung array string menjadi baris CSV, bungkus field yang mengandung koma */
    static String gabungCsv(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');
            String f = fields[i] == null ? "" : fields[i];
            if (f.contains(",") || f.contains("\"") || f.contains("\n")) {
                sb.append('"').append(f.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(f);
            }
        }
        return sb.toString();
    }

    /** Buat file CSV default jika belum ada */
    static void inisialisasiJikaKosong() {
        new File(DIR).mkdirs();

        // akun.csv
        if (!new File(FILE_AKUN).exists()) {
            String h = "username,password,nama_lengkap,role,tanggal_daftar";
            List<String[]> rows = new ArrayList<>();
            String today = LocalDate.now().toString();
            rows.add(new String[]{"admin",     "admin123",  "Administrator Sistem", "PENGAWAS", today});
            rows.add(new String[]{"pengawas1", "pengawas1", "Budi Santoso",         "PENGAWAS", today});
            rows.add(new String[]{"operator1", "operator1", "Andi Pratama",         "USER",     today});
            rows.add(new String[]{"operator2", "operator2", "Siti Rahayu",          "USER",     today});
            rows.add(new String[]{"operator3", "operator3", "Reza Firmansyah",      "USER",     today});
            tulis(FILE_AKUN, h, rows);
        }

        // produk.csv
        if (!new File(FILE_PRODUK).exists()) {
            String h = "id_produk,nama_produk,kategori,berat_standar,panjang_standar,lebar_standar";
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"PRD-001","Piston Silinder 4-Tak",      "Mesin",       "450.0", "8.5",  "8.5"});
            rows.add(new String[]{"PRD-002","Blok Silinder Aluminium",     "Mesin",      "3200.0","35.0", "22.0"});
            rows.add(new String[]{"PRD-003","Kepala Silinder Cast Iron",   "Mesin",      "2800.0","33.0", "20.0"});
            rows.add(new String[]{"PRD-004","Roda Gigi Transmisi Manual",  "Transmisi",   "380.0","12.0", "12.0"});
            rows.add(new String[]{"PRD-005","Poros Transmisi CVT",         "Transmisi",   "650.0","40.0",  "4.5"});
            rows.add(new String[]{"PRD-006","Pegas Koil Depan",            "Suspensi",    "820.0","30.0",  "6.0"});
            rows.add(new String[]{"PRD-007","Arm Suspensi Bawah",          "Suspensi",   "1100.0","42.0", "18.0"});
            rows.add(new String[]{"PRD-008","Kampas Rem Cakram Depan",     "Rem",         "310.0","14.0",  "6.5"});
            rows.add(new String[]{"PRD-009","Kaliper Rem Hidrolik",        "Rem",         "720.0","16.0", "10.0"});
            rows.add(new String[]{"PRD-010","Panel Pintu Kiri Depan",      "Bodi",       "4500.0","95.0", "75.0"});
            rows.add(new String[]{"PRD-011","Kap Mesin Stamping",          "Bodi",       "6200.0","120.0","95.0"});
            rows.add(new String[]{"PRD-012","Alternator 12V 90A",          "Kelistrikan","3800.0","20.0", "15.0"});
            tulis(FILE_PRODUK, h, rows);
        }

        // hasil_inspeksi.csv
        if (!new File(FILE_INSPEKSI).exists()) {
            String h = "id_inspeksi,id_produk,tanggal_inspeksi,nama_inspektur,berat_aktual,panjang_aktual,lebar_aktual,jumlah_cacat,catatan_tambahan,status";
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"INS-001","PRD-001","2026-04-01","Andi Pratama",    "449.5", "8.5",  "8.5",  "0","Dalam toleransi",              "LOLOS"});
            rows.add(new String[]{"INS-002","PRD-002","2026-04-02","Siti Rahayu",    "3198.0","35.1", "22.0",  "0","OK",                           "LOLOS"});
            rows.add(new String[]{"INS-003","PRD-004","2026-04-03","Andi Pratama",    "381.0","12.0", "11.9",  "0","Standar terpenuhi",            "LOLOS"});
            rows.add(new String[]{"INS-004","PRD-006","2026-04-05","Reza Firmansyah", "820.0","30.0",  "6.0",  "0","",                             "LOLOS"});
            rows.add(new String[]{"INS-005","PRD-008","2026-04-06","Siti Rahayu",     "309.5","14.0",  "6.5",  "0","Sesuai spesifikasi",           "LOLOS"});
            rows.add(new String[]{"INS-006","PRD-009","2026-04-07","Andi Pratama",    "720.0","16.0", "10.0",  "0","",                             "LOLOS"});
            rows.add(new String[]{"INS-007","PRD-010","2026-04-08","Reza Firmansyah","4502.0","95.2", "75.1",  "0","Panel mulus",                  "LOLOS"});
            rows.add(new String[]{"INS-008","PRD-012","2026-04-10","Siti Rahayu",    "3795.0","20.0", "15.0",  "0","Output tegangan normal",       "LOLOS"});
            rows.add(new String[]{"INS-009","PRD-003","2026-04-12","Andi Pratama",   "2803.0","33.1", "20.0",  "0","",                             "LOLOS"});
            rows.add(new String[]{"INS-010","PRD-005","2026-04-14","Reza Firmansyah", "651.0","40.0",  "4.5",  "0","Toleransi terpenuhi",          "LOLOS"});
            rows.add(new String[]{"INS-011","PRD-001","2026-04-15","Andi Pratama",    "480.0", "8.5",  "8.5",  "2","Berat melebihi batas 5%",      "GAGAL"});
            rows.add(new String[]{"INS-012","PRD-002","2026-04-16","Siti Rahayu",    "3100.0","35.9", "22.5",  "1","Deviasi dimensi panjang >2%",  "GAGAL"});
            rows.add(new String[]{"INS-013","PRD-004","2026-04-17","Reza Firmansyah", "400.0","12.5", "12.3",  "3","Cacat permukaan gigi",         "GAGAL"});
            rows.add(new String[]{"INS-014","PRD-007","2026-04-18","Andi Pratama",   "1200.0","43.0", "18.5",  "0","Berat dan dimensi OOB",        "GAGAL"});
            rows.add(new String[]{"INS-015","PRD-008","2026-04-20","Siti Rahayu",     "350.0","14.5",  "7.0",  "2","Kampas terlalu tebal",         "GAGAL"});
            rows.add(new String[]{"INS-016","PRD-011","2026-04-21","Reza Firmansyah","6500.0","122.0","97.0",  "1","Ada penyok kecil di sudut",    "GAGAL"});
            rows.add(new String[]{"INS-017","PRD-012","2026-04-22","Andi Pratama",   "4100.0","20.5", "15.5",  "0","Berat jauh di atas standar",   "GAGAL"});
            rows.add(new String[]{"INS-018","PRD-003","2026-04-24","Siti Rahayu",    "2650.0","32.0", "19.5",  "1","Permukaan kasar berlebihan",   "GAGAL"});
            rows.add(new String[]{"INS-019","PRD-005","2026-05-01","Reza Firmansyah", "652.0","40.1",  "4.5",  "0","Masih dalam toleransi",        "LOLOS"});
            rows.add(new String[]{"INS-020","PRD-010","2026-05-05","Andi Pratama",   "4600.0","96.5", "76.0",  "1","Deviasi dimensi & ada goresan","GAGAL"});
            tulis(FILE_INSPEKSI, h, rows);
        }
    }
}

// ============================================================
// [WEEK 3-4] ENUM ROLE PENGGUNA
// ============================================================
enum RolePengguna {
    PENGAWAS, USER
}

// ============================================================
// [WEEK 3-4] CLASS AKUN (Encapsulation)
// ============================================================
class Akun {
    private String username;
    private String password;
    private String namaLengkap;
    private RolePengguna role;
    private LocalDate tanggalDaftar;

    public Akun(String username, String password, String namaLengkap, RolePengguna role) {
        this.username      = username;
        this.password      = password;
        this.namaLengkap   = namaLengkap;
        this.role          = role;
        this.tanggalDaftar = LocalDate.now();
    }

    public String       getUsername()      { return username; }
    public String       getPassword()      { return password; }
    public String       getNamaLengkap()   { return namaLengkap; }
    public RolePengguna getRole()          { return role; }
    public LocalDate    getTanggalDaftar() { return tanggalDaftar; }

    public boolean verifikasiPassword(String pass) {
        return this.password.equals(pass);
    }

    @Override
    public String toString() {
        return namaLengkap + " (" + role.name() + ")";
    }
}

// ============================================================
// [WEEK 3-4] MANAJEMEN AKUN (Singleton-like) — berbasis CSV
// ============================================================
class AkunManager {

    static final String HEADER = "username,password,nama_lengkap,role,tanggal_daftar";

    public AkunManager() {
        CsvStorage.inisialisasiJikaKosong();
    }

    public Akun login(String username, String password) throws LoginGagalException {
        for (String[] r : CsvStorage.baca(CsvStorage.FILE_AKUN)) {
            if (r.length < 4) continue;
            if (r[0].equalsIgnoreCase(username)) {
                if (r[1].equals(password)) {
                    return new Akun(r[0], r[1], r[2], RolePengguna.valueOf(r[3].toUpperCase()));
                } else {
                    throw new LoginGagalException("Password salah untuk username: " + username);
                }
            }
        }
        throw new LoginGagalException("Username tidak ditemukan: " + username);
    }

    public boolean usernameAda(String username) {
        for (String[] r : CsvStorage.baca(CsvStorage.FILE_AKUN)) {
            if (r.length > 0 && r[0].equalsIgnoreCase(username)) return true;
        }
        return false;
    }

    public void daftarAkun(Akun akun) throws RegistrasiGagalException {
        if (usernameAda(akun.getUsername()))
            throw new RegistrasiGagalException("Username sudah digunakan: " + akun.getUsername());
        CsvStorage.tambahBaris(CsvStorage.FILE_AKUN, HEADER, new String[]{
            akun.getUsername(), akun.getPassword(), akun.getNamaLengkap(),
            akun.getRole().name(), akun.getTanggalDaftar().toString()
        });
    }

    public List<Akun> getDaftarAkun() {
        List<Akun> daftar = new ArrayList<>();
        for (String[] r : CsvStorage.baca(CsvStorage.FILE_AKUN)) {
            if (r.length >= 4)
                daftar.add(new Akun(r[0], r[1], r[2], RolePengguna.valueOf(r[3].toUpperCase())));
        }
        return daftar;
    }
}

// ============================================================
// [WEEK 7] CUSTOM EXCEPTIONS
// ============================================================
class LoginGagalException extends Exception {
    public LoginGagalException(String pesan) { super(pesan); }
}

class RegistrasiGagalException extends Exception {
    public RegistrasiGagalException(String pesan) { super(pesan); }
}

class DataTidakValidException extends Exception {
    public DataTidakValidException(String pesan) { super(pesan); }
}

// ============================================================
// [WEEK 3-4] CLASS PRODUK
// ============================================================
class Produk {
    private String idProduk;
    private String namaProduk;
    private String kategori;
    private double beratStandar;
    private double panjangStandar;
    private double lebarStandar;

    public Produk(String idProduk, String namaProduk, String kategori,
                  double beratStandar, double panjangStandar, double lebarStandar) {
        this.idProduk       = idProduk;
        this.namaProduk     = namaProduk;
        this.kategori       = kategori;
        this.beratStandar   = beratStandar;
        this.panjangStandar = panjangStandar;
        this.lebarStandar   = lebarStandar;
    }

    public String getIdProduk()       { return idProduk; }
    public String getNamaProduk()     { return namaProduk; }
    public String getKategori()       { return kategori; }
    public double getBeratStandar()   { return beratStandar; }
    public double getPanjangStandar() { return panjangStandar; }
    public double getLebarStandar()   { return lebarStandar; }

    @Override public String toString() { return idProduk + " - " + namaProduk; }
}

// ============================================================
// [WEEK 5] INHERITANCE – Hasil Inspeksi
// ============================================================
class HasilInspeksi {
    protected String idInspeksi;
    protected Produk produk;
    protected String tanggalInspeksi;
    protected String namaInspektur;
    protected double beratAktual;
    protected double panjangAktual;
    protected double lebarAktual;

    public HasilInspeksi(String idInspeksi, Produk produk, String tanggalInspeksi,
                         String namaInspektur, double beratAktual,
                         double panjangAktual, double lebarAktual) {
        this.idInspeksi      = idInspeksi;
        this.produk          = produk;
        this.tanggalInspeksi = tanggalInspeksi;
        this.namaInspektur   = namaInspektur;
        this.beratAktual     = beratAktual;
        this.panjangAktual   = panjangAktual;
        this.lebarAktual     = lebarAktual;
    }

    public double getSelisihBerat()   { return beratAktual - produk.getBeratStandar(); }
    public double getDeviasiPanjang() { return ((panjangAktual - produk.getPanjangStandar()) / produk.getPanjangStandar()) * 100; }
    public double getDeviasiLebar()   { return ((lebarAktual  - produk.getLebarStandar())   / produk.getLebarStandar())   * 100; }

    public String getIdInspeksi()      { return idInspeksi; }
    public Produk getProduk()          { return produk; }
    public String getTanggalInspeksi() { return tanggalInspeksi; }
    public String getNamaInspektur()   { return namaInspektur; }
    public double getBeratAktual()     { return beratAktual; }
    public double getPanjangAktual()   { return panjangAktual; }
    public double getLebarAktual()     { return lebarAktual; }
}

// [WEEK 5] Subclass
class InspeksiDetail extends HasilInspeksi {
    private String  catatanTambahan;
    private int     jumlahCacat;
    private boolean lolosQC;

    public InspeksiDetail(String idInspeksi, Produk produk, String tanggalInspeksi,
                          String namaInspektur, double beratAktual,
                          double panjangAktual, double lebarAktual,
                          int jumlahCacat, String catatanTambahan) {
        super(idInspeksi, produk, tanggalInspeksi, namaInspektur,
              beratAktual, panjangAktual, lebarAktual);
        this.jumlahCacat     = jumlahCacat;
        this.catatanTambahan = catatanTambahan;
        // TOLERANSI QC PRESISI: Selisih Berat maks 5%, Deviasi Dimensi maks 2%, Tanpa Cacat Fisik (0)
        this.lolosQC = (Math.abs(getSelisihBerat())   <= (produk.getBeratStandar() * 0.05))
                    && (Math.abs(getDeviasiPanjang())  <= 2.0)
                    && (Math.abs(getDeviasiLebar())    <= 2.0)
                    && (jumlahCacat == 0);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s | %s | Lolos: %s",
                idInspeksi, produk.getNamaProduk(), tanggalInspeksi,
                namaInspektur, lolosQC ? "YA" : "TIDAK");
    }

    public boolean isLolosQC()          { return lolosQC; }
    public int     getJumlahCacat()     { return jumlahCacat; }
    public String  getCatatanTambahan() { return catatanTambahan; }
}

// ============================================================
// [WEEK 6] INTERFACE Statistikal
// ============================================================
interface Statistikal {
    double hitungRataRata(List<Double> data);
    double hitungNilaiMax(List<Double> data);
    double hitungNilaiMin(List<Double> data);
    String generateLaporan();
}

// ============================================================
// [WEEK 8] QCDataManager + Collection + Sortir + Filter
// ============================================================
class QCDataManager implements Statistikal {
    private List<InspeksiDetail> daftarInspeksi  = new ArrayList<>();
    private List<Produk>         daftarProduk    = new ArrayList<>();
    private Map<String, Integer> totalPerKategori = new HashMap<>();
    private Map<String, Integer> lolosPerKategori = new HashMap<>();

    public void muatDataDariDatabase() {
        daftarProduk.clear();
        daftarInspeksi.clear();
        totalPerKategori.clear();
        lolosPerKategori.clear();

        // 1. Baca master produk dari produk.csv
        // Header: id_produk,nama_produk,kategori,berat_standar,panjang_standar,lebar_standar
        Map<String, Produk> mapProduk = new HashMap<>();
        for (String[] r : CsvStorage.baca(CsvStorage.FILE_PRODUK)) {
            if (r.length < 6) continue;
            try {
                Produk p = new Produk(r[0], r[1], r[2],
                    Double.parseDouble(r[3]), Double.parseDouble(r[4]), Double.parseDouble(r[5]));
                daftarProduk.add(p);
                mapProduk.put(r[0], p);
            } catch (NumberFormatException e) {
                System.err.println("Baris produk tidak valid: " + String.join(",", r));
            }
        }

        // 2. Baca hasil inspeksi dari hasil_inspeksi.csv
        // Header: id_inspeksi,id_produk,tanggal_inspeksi,nama_inspektur,
        //         berat_aktual,panjang_aktual,lebar_aktual,jumlah_cacat,catatan_tambahan,status
        for (String[] r : CsvStorage.baca(CsvStorage.FILE_INSPEKSI)) {
            if (r.length < 9) continue;
            Produk p = mapProduk.get(r[1]);
            if (p == null) continue;
            try {
                InspeksiDetail detail = new InspeksiDetail(
                    r[0], p, r[2], r[3],
                    Double.parseDouble(r[4]), Double.parseDouble(r[5]),
                    Double.parseDouble(r[6]), Integer.parseInt(r[7]),
                    r.length > 8 ? r[8] : ""
                );
                daftarInspeksi.add(detail);
                String kat = p.getKategori();
                totalPerKategori.put(kat, totalPerKategori.getOrDefault(kat, 0) + 1);
                if (detail.isLolosQC())
                    lolosPerKategori.put(kat, lolosPerKategori.getOrDefault(kat, 0) + 1);
            } catch (NumberFormatException e) {
                System.err.println("Baris inspeksi tidak valid: " + String.join(",", r));
            }
        }
    }

    public void tambahProduk(Produk p) {
        // Produk baru disimpan ke produk.csv
        CsvStorage.tambahBaris(CsvStorage.FILE_PRODUK,
            "id_produk,nama_produk,kategori,berat_standar,panjang_standar,lebar_standar",
            new String[]{p.getIdProduk(), p.getNamaProduk(), p.getKategori(),
                String.valueOf(p.getBeratStandar()), String.valueOf(p.getPanjangStandar()),
                String.valueOf(p.getLebarStandar())});
    }

    public void tambahInspeksi(InspeksiDetail ins) throws DataTidakValidException {
        if (ins.getBeratAktual() <= 0)
            throw new DataTidakValidException("Berat aktual tidak boleh nol atau negatif!");
        if (ins.getPanjangAktual() <= 0 || ins.getLebarAktual() <= 0)
            throw new DataTidakValidException("Dimensi aktual tidak boleh nol atau negatif!");

        String[] row = {
            ins.getIdInspeksi(),
            ins.getProduk().getIdProduk(),
            ins.getTanggalInspeksi(),
            ins.getNamaInspektur(),
            String.valueOf(ins.getBeratAktual()),
            String.valueOf(ins.getPanjangAktual()),
            String.valueOf(ins.getLebarAktual()),
            String.valueOf(ins.getJumlahCacat()),
            ins.getCatatanTambahan(),
            ins.isLolosQC() ? "LOLOS" : "GAGAL"
        };
        CsvStorage.tambahBaris(CsvStorage.FILE_INSPEKSI,
            "id_inspeksi,id_produk,tanggal_inspeksi,nama_inspektur," +
            "berat_aktual,panjang_aktual,lebar_aktual,jumlah_cacat,catatan_tambahan,status", row);
        muatDataDariDatabase();
    }

    public void hapusInspeksi(String idInspeksi) throws DataTidakValidException {
        List<String[]> rows = CsvStorage.baca(CsvStorage.FILE_INSPEKSI);
        List<String[]> baru = new ArrayList<>();
        boolean ditemukan = false;
        for (String[] r : rows) {
            if (r.length > 0 && r[0].equals(idInspeksi)) { ditemukan = true; }
            else baru.add(r);
        }
        if (!ditemukan) throw new DataTidakValidException("ID tidak ditemukan: " + idInspeksi);
        CsvStorage.tulis(CsvStorage.FILE_INSPEKSI,
            "id_inspeksi,id_produk,tanggal_inspeksi,nama_inspektur," +
            "berat_aktual,panjang_aktual,lebar_aktual,jumlah_cacat,catatan_tambahan,status", baru);
        muatDataDariDatabase();
    }

    public void updateInspeksi(String idInspeksi, double berat, double panjang, double lebar,
                               int cacat, String catatan, String tanggal, String inspektur) throws DataTidakValidException {
        if (berat <= 0 || panjang <= 0 || lebar <= 0)
            throw new DataTidakValidException("Nilai dimensi tidak boleh nol atau negatif!");

        // Cari standar produk dari in-memory list (sudah dimuat)
        String idProduk = null;
        String statusBaru = "GAGAL";
        for (InspeksiDetail ins : daftarInspeksi) {
            if (ins.getIdInspeksi().equals(idInspeksi)) {
                idProduk = ins.getProduk().getIdProduk();
                Produk p = ins.getProduk();
                boolean lolos = (Math.abs(berat   - p.getBeratStandar())   <= p.getBeratStandar()   * 0.05)
                             && (Math.abs((panjang - p.getPanjangStandar()) / p.getPanjangStandar() * 100) <= 2.0)
                             && (Math.abs((lebar   - p.getLebarStandar())   / p.getLebarStandar()   * 100) <= 2.0)
                             && (cacat == 0);
                statusBaru = lolos ? "LOLOS" : "GAGAL";
                break;
            }
        }
        if (idProduk == null) throw new DataTidakValidException("ID inspeksi tidak ditemukan: " + idInspeksi);

        // Tulis ulang CSV dengan baris yang sudah diupdate
        List<String[]> rows = CsvStorage.baca(CsvStorage.FILE_INSPEKSI);
        for (String[] r : rows) {
            if (r.length > 0 && r[0].equals(idInspeksi)) {
                r[2] = tanggal;
                r[3] = inspektur;
                r[4] = String.valueOf(berat);
                r[5] = String.valueOf(panjang);
                r[6] = String.valueOf(lebar);
                r[7] = String.valueOf(cacat);
                r[8] = catatan;
                if (r.length > 9) r[9] = statusBaru;
            }
        }
        CsvStorage.tulis(CsvStorage.FILE_INSPEKSI,
            "id_inspeksi,id_produk,tanggal_inspeksi,nama_inspektur," +
            "berat_aktual,panjang_aktual,lebar_aktual,jumlah_cacat,catatan_tambahan,status", rows);
        muatDataDariDatabase();
    }

    // ============================================================
    // [WEEK 9] SORTIR TANGGAL (ascending / descending)
    // ============================================================
    public List<InspeksiDetail> getDaftarSortirTanggal(boolean ascending) {
        List<InspeksiDetail> sorted = new ArrayList<>(daftarInspeksi);
        sorted.sort((a, b) -> {
            try {
                LocalDate da = LocalDate.parse(a.getTanggalInspeksi());
                LocalDate db = LocalDate.parse(b.getTanggalInspeksi());
                return ascending ? da.compareTo(db) : db.compareTo(da);
            } catch (DateTimeParseException e) {
                return 0;
            }
        });
        return sorted;
    }

    // ============================================================
    // FILTER BATAS TANGGAL (min date – max date)
    // ============================================================
    public List<InspeksiDetail> filterTanggal(LocalDate tglMin, LocalDate tglMax) {
        List<InspeksiDetail> hasil = new ArrayList<>();
        for (InspeksiDetail ins : daftarInspeksi) {
            try {
                LocalDate tgl = LocalDate.parse(ins.getTanggalInspeksi());
                if ((tglMin == null || !tgl.isBefore(tglMin))
                 && (tglMax == null || !tgl.isAfter(tglMax))) {
                    hasil.add(ins);
                }
            } catch (DateTimeParseException e) { /* skip */ }
        }
        return hasil;
    }

    // ============================================================
    // FILTER BATAS BERAT (min – max)
    // ============================================================
    public List<InspeksiDetail> filterBerat(double min, double max) {
        List<InspeksiDetail> hasil = new ArrayList<>();
        for (InspeksiDetail ins : daftarInspeksi) {
            if (ins.getBeratAktual() >= min && ins.getBeratAktual() <= max) {
                hasil.add(ins);
            }
        }
        return hasil;
    }

    // Gabungan filter tanggal + berat + sort
    public List<InspeksiDetail> filterDanSortir(
            LocalDate tglMin, LocalDate tglMax,
            double beratMin,  double beratMax,
            boolean ascending) {

        List<InspeksiDetail> hasil = new ArrayList<>(daftarInspeksi);

        // Filter tanggal
        if (tglMin != null || tglMax != null) {
            hasil = hasil.stream().filter(ins -> {
                try {
                    LocalDate tgl = LocalDate.parse(ins.getTanggalInspeksi());
                    boolean okMin = tglMin == null || !tgl.isBefore(tglMin);
                    boolean okMax = tglMax == null || !tgl.isAfter(tglMax);
                    return okMin && okMax;
                } catch (DateTimeParseException e) { return false; }
            }).collect(Collectors.toList());
        }

        // Filter berat
        if (beratMin > 0 || beratMax < Double.MAX_VALUE) {
            hasil = hasil.stream()
                .filter(ins -> ins.getBeratAktual() >= beratMin && ins.getBeratAktual() <= beratMax)
                .collect(Collectors.toList());
        }

        // Sort tanggal
        hasil.sort((a, b) -> {
            try {
                LocalDate da = LocalDate.parse(a.getTanggalInspeksi());
                LocalDate db = LocalDate.parse(b.getTanggalInspeksi());
                return ascending ? da.compareTo(db) : db.compareTo(da);
            } catch (DateTimeParseException e) { return 0; }
        });

        return hasil;
    }

    public int    getTotalInspeksi()  { return daftarInspeksi.size(); }
    public int    getTotalLolos()     {
        int n = 0;
        for (InspeksiDetail ins : daftarInspeksi) if (ins.isLolosQC()) n++;
        return n;
    }
    public int    getTotalGagal()     { return getTotalInspeksi() - getTotalLolos(); }
    public double getPersentaseLolos() {
        if (getTotalInspeksi() == 0) return 0;
        return (getTotalLolos() * 100.0) / getTotalInspeksi();
    }

    public List<InspeksiDetail> getDaftarInspeksi()    { return daftarInspeksi; }
    public List<Produk>         getDaftarProduk()      { return daftarProduk; }
    public Map<String, Integer> getTotalPerKategori()  { return totalPerKategori; }
    public Map<String, Integer> getLolosPerKategori()  { return lolosPerKategori; }

    public List<Double> getDataBerat() {
        List<Double> data = new ArrayList<>();
        for (InspeksiDetail ins : daftarInspeksi) data.add(ins.getBeratAktual());
        return data;
    }

    @Override public double hitungRataRata(List<Double> data) {
        if (data.isEmpty()) return 0;
        double sum = 0;
        for (double d : data) sum += d;
        return sum / data.size();
    }
    @Override public double hitungNilaiMax(List<Double> data) {
        if (data.isEmpty()) return 0;
        double max = data.get(0);
        for (double d : data) if (d > max) max = d;
        return max;
    }
    @Override public double hitungNilaiMin(List<Double> data) {
        if (data.isEmpty()) return 0;
        double min = data.get(0);
        for (double d : data) if (d < min) min = d;
        return min;
    }

    @Override public String generateLaporan() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== LAPORAN QUALITY CONTROL AUTOMOTIVE =====\n");
        sb.append(String.format("Tanggal Laporan  : %s\n", LocalDate.now()));
        sb.append(String.format("Total Inspeksi   : %d\n", getTotalInspeksi()));
        sb.append(String.format("Total Lolos QC   : %d\n", getTotalLolos()));
        sb.append(String.format("Total Gagal QC   : %d\n", getTotalGagal()));
        sb.append(String.format("Persentase Lolos : %.2f%%\n", getPersentaseLolos()));
        List<Double> berat = getDataBerat();
        if (!berat.isEmpty()) {
            sb.append(String.format("Rata-rata Berat  : %.2f g\n", hitungRataRata(berat)));
            sb.append(String.format("Berat Maksimum   : %.2f g\n", hitungNilaiMax(berat)));
            sb.append(String.format("Berat Minimum    : %.2f g\n", hitungNilaiMin(berat)));
        }
        sb.append("\n--- Per Kelompok Kategori Otomotif ---\n");
        for (Map.Entry<String, Integer> entry : totalPerKategori.entrySet()) {
            String kat  = entry.getKey();
            int total   = entry.getValue();
            int lolos   = lolosPerKategori.getOrDefault(kat, 0);
            double pct  = total == 0 ? 0 : (lolos * 100.0 / total);
            sb.append(String.format("  %-20s: %d/%d (%.1f%%)\n", kat, lolos, total, pct));
        }
        return sb.toString();
    }
}

// ============================================================
// [WEEK 10] PANEL BAR CHART (Modern Resizable Layout)
// ============================================================
class PanelBarChart extends JPanel {
    private Map<String, Integer> dataLolos;
    private Map<String, Integer> dataTotal;
    private String judul;

    public PanelBarChart(String judul, Map<String, Integer> dataLolos, Map<String, Integer> dataTotal) {
        this.judul     = judul;
        this.dataLolos = dataLolos;
        this.dataTotal = dataTotal;
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int margin = 45, barW = 32, gap = 15;
        int chartH = h - margin - 20;

        // Grid & Aksis
        g2.setColor(new Color(226, 232, 240));
        g2.drawLine(margin, h - margin, w - 15, h - margin);

        if (dataTotal == null || dataTotal.isEmpty()) {
            g2.setFont(new Font("Plus Jakarta Sans", Font.ITALIC, 11));
            g2.setColor(Color.GRAY);
            g2.drawString("Belum ada data historis", margin + 10, h / 2);
            return;
        }

        int x = margin + gap;
        int maxVal = dataTotal.values().stream().max(Integer::compare).orElse(1);

        for (String kat : dataTotal.keySet()) {
            int total = dataTotal.getOrDefault(kat, 0);
            int lolos = dataLolos.getOrDefault(kat, 0);
            int gagal = total - lolos;
            if (total == 0) continue;

            double skala  = (double) chartH / Math.max(maxVal, 1);
            int hLolos = (int)(lolos * skala);
            int hGagal = (int)(gagal * skala);

            // Batang Lolos (Hijau Emerald)
            g2.setColor(new Color(16, 185, 129));
            g2.fillRect(x, h - margin - hLolos, barW, hLolos);

            // Batang Gagal (Merah Rose)
            g2.setColor(new Color(244, 63, 94));
            g2.fillRect(x + 4, h - margin - hLolos - hGagal, barW - 8, hGagal);

            g2.setColor(new Color(71, 85, 105));
            g2.setFont(new Font("Plus Jakarta Sans", Font.PLAIN, 10));
            String shortKat = kat.length() > 6 ? kat.substring(0, 5) + "." : kat;
            g2.drawString(shortKat, x, h - margin + 15);

            x += barW + gap;
            if (x > w - 20) break;
        }
    }
}

// ============================================================
// [WEEK 10-11] PIE CHART
// ============================================================
class PanelPieChart extends JPanel {
    private int lolos, gagal;
    public PanelPieChart(int lolos, int gagal) {
        this.lolos = lolos; this.gagal = gagal;
        setBackground(Color.WHITE);
    }
    public void update(int lolos, int gagal) { this.lolos = lolos; this.gagal = gagal; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int total = lolos + gagal;
        int cx = getWidth()/2, cy = getHeight()/2 - 10, r = 50;

        if (total == 0) {
            g2.setColor(new Color(241, 245, 249));
            g2.fillOval(cx-r, cy-r, r*2, r*2);
            return;
        }
        int arcL = (int) Math.round(360.0 * lolos / total);
        g2.setColor(new Color(16, 185, 129));
        g2.fillArc(cx-r, cy-r, r*2, r*2, 90, -arcL);
        g2.setColor(new Color(244, 63, 94));
        g2.fillArc(cx-r, cy-r, r*2, r*2, 90-arcL, -(360-arcL));
        
        // Inner Hole (Donut style)
        g2.setColor(Color.WHITE);
        g2.fillOval(cx-25, cy-25, 50, 50);
    }
}

// ============================================================
// [WEEK 10-11] SCREEN LOGIN (Premium Premium Look UI)
// ============================================================
class FrameLogin extends JFrame {
    private AkunManager  akunManager;
    private JTextField   tfUsername;
    private JPasswordField tfPassword;
    private JComboBox<String> cbRole;

    private static final Color DARK_BLUE = new Color(15, 23, 42);
    private static final Color SLATE_PRIMARY = new Color(30, 41, 59);

    public FrameLogin(AkunManager mgr) {
        this.akunManager = mgr;
        setTitle("Login Portal - Quality Control Dashboard");
        setSize(420, 580);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        buatUI();
    }

    private void buatUI() {
        JPanel bg = new JPanel(new GridBagLayout());
        bg.setBackground(new Color(241, 245, 249));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(30, 35, 25, 35)));
        card.setPreferredSize(new Dimension(360, 460));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.gridwidth = 1; gbc.weightx = 1.0;

        // Logo area
        JLabel logo = new JLabel("⚙", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 38));
        logo.setForeground(DARK_BLUE);

        JLabel title = new JLabel("QC SYSTEM LOGIN", SwingConstants.CENTER);
        title.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 18));
        title.setForeground(DARK_BLUE);

        JLabel sub = new JLabel("Automotive Manufacturing", SwingConstants.CENTER);
        sub.setFont(new Font("Plus Jakarta Sans", Font.PLAIN, 11));
        sub.setForeground(new Color(148, 163, 184));
        sub.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        cbRole = new JComboBox<>(new String[]{"USER – Operator/Inspektur", "PENGAWAS – Supervisor/Admin"});
        cbRole.setFont(new Font("Plus Jakarta Sans", Font.PLAIN, 11));

        tfUsername = new JTextField();
        tfPassword = new JPasswordField();

        for (JComponent tf : new JComponent[]{tfUsername, tfPassword, cbRole}) {
            tf.setFont(new Font("Plus Jakarta Sans", Font.PLAIN, 12));
            if (tf instanceof JTextField) {
                ((JTextField) tf).setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(203, 213, 225)),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            }
        }

        JButton btnLogin = new JButton("Sign In");
        btnLogin.setBackground(DARK_BLUE);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 13));
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setPreferredSize(new Dimension(0, 40));
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.addActionListener(e -> prosesLogin());

        JButton btnDaftar = new JButton("Belum punya akun? Daftar di sini");
        btnDaftar.setForeground(new Color(59, 130, 246));
        btnDaftar.setContentAreaFilled(false);
        btnDaftar.setBorderPainted(false);
        btnDaftar.setFont(new Font("Plus Jakarta Sans", Font.PLAIN, 11));
        btnDaftar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDaftar.addActionListener(e -> bukaRegistrasi());

        int y = 0;
        gbc.gridx = 0; gbc.gridy = y++; card.add(logo, gbc);
        gbc.gridy = y++; card.add(title, gbc);
        gbc.gridy = y++; card.add(sub, gbc);
        gbc.gridy = y++; card.add(buatLoginLabel("Akses Level"), gbc);
        gbc.gridy = y++; card.add(cbRole, gbc);
        gbc.gridy = y++; card.add(buatLoginLabel("Username"), gbc);
        gbc.gridy = y++; card.add(tfUsername, gbc);
        gbc.gridy = y++; card.add(buatLoginLabel("Password"), gbc);
        gbc.gridy = y++; card.add(tfPassword, gbc);
        gbc.gridy = y++; gbc.insets = new Insets(15, 0, 5, 0); card.add(btnLogin, gbc);
        gbc.gridy = y++; gbc.insets = new Insets(0, 0, 0, 0); card.add(btnDaftar, gbc);

        bg.add(card);
        setContentPane(bg);
        getRootPane().setDefaultButton(btnLogin);
    }

    private JLabel buatLoginLabel(String teks) {
        JLabel l = new JLabel(teks);
        l.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 11));
        l.setForeground(new Color(71, 85, 105));
        l.setBorder(BorderFactory.createEmptyBorder(4, 0, 2, 0));
        return l;
    }

    private void prosesLogin() {
        String username = tfUsername.getText().trim();
        String password = new String(tfPassword.getPassword());
        String roleStr  = (String) cbRole.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Isi username & password!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Akun akun = akunManager.login(username, password);
            boolean isPengawasSelected = roleStr.startsWith("PENGAWAS");
            boolean isPengawas = akun.getRole() == RolePengguna.PENGAWAS;

            if (isPengawasSelected && !isPengawas) {
                JOptionPane.showMessageDialog(this, "Akses ditolak! Bukan Pengawas.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!isPengawasSelected && isPengawas) {
                JOptionPane.showMessageDialog(this, "Gunakan login akses Pengawas.", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            dispose();
            new QualityControlDashboard(akun).setVisible(true);
        } catch (LoginGagalException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Gagal", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void bukaRegistrasi() {
        new FrameRegistrasi(akunManager, this).setVisible(true);
        setVisible(false);
    }
}

// ============================================================
// [WEEK 10-11] SCREEN REGISTRASI
// ============================================================
class FrameRegistrasi extends JFrame {
    private AkunManager akunManager;
    private JFrame      parentFrame;
    private JTextField     tfNama, tfUsername;
    private JPasswordField tfPassword, tfKonfirmasi;
    private JComboBox<String> cbRole;

    public FrameRegistrasi(AkunManager mgr, JFrame parent) {
        this.akunManager = mgr;
        this.parentFrame = parent;
        setTitle("Registrasi Anggota Baru");
        setSize(430, 580);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { parentFrame.setVisible(true); }
        });
        buatUI();
    }

    private void buatUI() {
        JPanel bg = new JPanel(new GridBagLayout());
        bg.setBackground(new Color(241, 245, 249));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(25, 35, 25, 35)));
        card.setPreferredSize(new Dimension(380, 510));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.gridwidth = 1; gbc.weightx = 1.0;

        JLabel title = new JLabel("PENDAFTARAN AKUN", SwingConstants.CENTER);
        title.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 17));
        title.setForeground(new Color(15, 23, 42));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        tfNama       = new JTextField();
        tfUsername   = new JTextField();
        tfPassword   = new JPasswordField();
        tfKonfirmasi = new JPasswordField();
        cbRole = new JComboBox<>(new String[]{"USER – Operator/Inspektur", "PENGAWAS – Supervisor/Admin"});
        cbRole.setFont(new Font("Plus Jakarta Sans", Font.PLAIN, 11));

        for (JTextField tf : new JTextField[]{tfNama, tfUsername, tfPassword, tfKonfirmasi}) {
            tf.setFont(new Font("Plus Jakarta Sans", Font.PLAIN, 12));
            tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        }

        JButton btnDaftar = new JButton("Register Now");
        btnDaftar.setBackground(new Color(16, 185, 129));
        btnDaftar.setForeground(Color.WHITE);
        btnDaftar.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 13));
        btnDaftar.setFocusPainted(false);
        btnDaftar.setBorderPainted(false);
        btnDaftar.setPreferredSize(new Dimension(0, 40));
        btnDaftar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDaftar.addActionListener(e -> prosesRegistrasi());

        int y = 0;
        gbc.gridx = 0; gbc.gridy = y++; card.add(title, gbc);
        gbc.gridy = y++; card.add(buatRegLabel("Nama Lengkap"), gbc);
        gbc.gridy = y++; card.add(tfNama, gbc);
        gbc.gridy = y++; card.add(buatRegLabel("Username"), gbc);
        gbc.gridy = y++; card.add(tfUsername, gbc);
        gbc.gridy = y++; card.add(buatRegLabel("Password"), gbc);
        gbc.gridy = y++; card.add(tfPassword, gbc);
        gbc.gridy = y++; card.add(buatRegLabel("Konfirmasi Password"), gbc);
        gbc.gridy = y++; card.add(tfKonfirmasi, gbc);
        gbc.gridy = y++; card.add(buatRegLabel("Role Hak Akses"), gbc);
        gbc.gridy = y++; card.add(cbRole, gbc);
        gbc.gridy = y++; gbc.insets = new Insets(18, 0, 0, 0); card.add(btnDaftar, gbc);

        bg.add(card);
        setContentPane(bg);
    }

    private JLabel buatRegLabel(String teks) {
        JLabel l = new JLabel(teks);
        l.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 11));
        l.setForeground(new Color(71, 85, 105));
        l.setBorder(BorderFactory.createEmptyBorder(4, 0, 2, 0));
        return l;
    }

    private void prosesRegistrasi() {
        String nama = tfNama.getText().trim();
        String username = tfUsername.getText().trim();
        String password = new String(tfPassword.getPassword());
        String konfirm = new String(tfKonfirmasi.getPassword());
        boolean isPengawas = ((String) cbRole.getSelectedItem()).startsWith("PENGAWAS");

        if (nama.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Data tidak boleh kosong!", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!password.equals(konfirm)) {
            JOptionPane.showMessageDialog(this, "Konfirmasi password tidak cocok!", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (isPengawas) {
            String kode = JOptionPane.showInputDialog(this, "Masukkan Kode Otorisasi Pengawas:");
            if (!"STMI2025".equals(kode)) {
                JOptionPane.showMessageDialog(this, "Kode salah!", "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        try {
            RolePengguna role = isPengawas ? RolePengguna.PENGAWAS : RolePengguna.USER;
            akunManager.daftarAkun(new Akun(username, password, nama, role));
            JOptionPane.showMessageDialog(this, "Pendaftaran berhasil! Silakan Login.");
            dispose();
            parentFrame.setVisible(true);
        } catch (RegistrasiGagalException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

// ============================================================
// [WEEK 10-11] MAIN FRAME DASHBOARD
// ============================================================
public class QualityControlDashboard extends JFrame {

    private Akun          akunAktif;
    private QCDataManager dataManager = new QCDataManager();

    private JTable          tabelInspeksi;
    private DefaultTableModel modelTabel;
    private PanelBarChart   panelBar;
    private PanelPieChart   panelPie;
    private JLabel          lblTotal, lblLolos, lblGagal, lblPersen;

    private JComboBox<Produk> cbProduk;
    private JTextField tfInspektur, tfTanggal, tfBerat, tfPanjang, tfLebar, tfCacat, tfCatatan;
    private JTextField  tfTglMin, tfTglMax, tfBeratMin, tfBeratMax;
    private JComboBox<String> cbSortTanggal;
    private JButton btnEdit, btnHapus; // CRUD toolbar untuk Pengawas

    private static final Color DARK_SLATE = new Color(15, 23, 42);
    private static final Color EMERALD_GREEN = new Color(16, 185, 129);
    private static final Color ROSE_RED = new Color(244, 63, 94);
    private static final Color BACKGROUND_BG = new Color(248, 250, 252);

    public QualityControlDashboard(Akun akun) {
        this.akunAktif = akun;
        dataManager.muatDataDariDatabase(); // Ambil real-data langsung dari database MySQL phpMyAdmin
        buatUI();
    }

    private void buatUI() {
        setTitle("Sistem Monitoring Mutu Quality Control Otomotif");
        setSize(1300, 780);
        setMinimumSize(new Dimension(1100, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BACKGROUND_BG);

        // HEADER
        add(buatPanelHeader(), BorderLayout.NORTH);

        // MAIN CONTENT dengan JTabbedPane
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 12));
        tabs.setBackground(BACKGROUND_BG);
        tabs.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

        // ── TAB 1: DASHBOARD (KPI + Charts) ──────────────────────
        tabs.addTab("  📊 Dashboard  ", buatTabDashboard());

        // ── TAB 2: INPUT INSPEKSI ─────────────────────────────────
        tabs.addTab("  ➕ Input Inspeksi  ", buatTabInput());

        // ── TAB 3: TABEL HISTORIS + FILTER ───────────────────────
        tabs.addTab("  📋 Riwayat & Filter  ", buatTabRiwayat());

        add(tabs, BorderLayout.CENTER);

        refreshTabel(dataManager.getDaftarInspeksi());
        refreshKPI();
    }

    // ── TAB 1: DASHBOARD ─────────────────────────────────────────
    private JPanel buatTabDashboard() {
        JPanel p = new JPanel(new BorderLayout(15, 15));
        p.setBackground(BACKGROUND_BG);
        p.setBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5));

        // KPI row
        p.add(buatPanelKPI(), BorderLayout.NORTH);

        // Charts di bawah KPI
        JPanel charts = new JPanel(new GridLayout(1, 2, 15, 0));
        charts.setBackground(BACKGROUND_BG);

        panelPie = new PanelPieChart(dataManager.getTotalLolos(), dataManager.getTotalGagal());
        panelPie.setBackground(Color.WHITE);
        panelPie.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240)),
            BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(8,8,8,8),
                "  Proporsi Kelulusan QC",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Plus Jakarta Sans", Font.BOLD, 12),
                new Color(71, 85, 105))));
        panelPie.setPreferredSize(new Dimension(0, 320));

        panelBar = new PanelBarChart("Distribusi Mutu per Kategori", dataManager.getLolosPerKategori(), dataManager.getTotalPerKategori());
        panelBar.setBackground(Color.WHITE);
        panelBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240)),
            BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(8,8,8,8),
                "  Grafik Evaluasi Kategori",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Plus Jakarta Sans", Font.BOLD, 12),
                new Color(71, 85, 105))));

        charts.add(panelPie);
        charts.add(panelBar);
        p.add(charts, BorderLayout.CENTER);
        return p;
    }

    // ── TAB 2: FORM INPUT ────────────────────────────────────────
    private JPanel buatTabInput() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(BACKGROUND_BG);

        JPanel card = new JPanel(new BorderLayout(0, 15));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(20, 28, 20, 28)));
        card.setPreferredSize(new Dimension(520, 500));

        JLabel header = new JLabel("Form Input Data Inspeksi");
        header.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 16));
        header.setForeground(DARK_SLATE);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        card.add(header, BorderLayout.NORTH);
        card.add(buatPanelForm(), BorderLayout.CENTER);
        card.add(buatPanelTombol(), BorderLayout.SOUTH);

        outer.add(card);
        return outer;
    }

    // ── TAB 3: RIWAYAT & FILTER ──────────────────────────────────
    private JPanel buatTabRiwayat() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(BACKGROUND_BG);
        p.setBorder(BorderFactory.createEmptyBorder(12, 5, 5, 5));

        p.add(buatPanelFilter(), BorderLayout.NORTH);
        p.add(buatPanelTabel(), BorderLayout.CENTER);

        // Toolbar CRUD khusus Pengawas
        if (akunAktif.getRole() == RolePengguna.PENGAWAS) {
            p.add(buatPanelCRUD(), BorderLayout.SOUTH);
        }
        return p;
    }

    private JPanel buatPanelCRUD() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));

        JLabel lbl = new JLabel("🛡 Aksi Pengawas:");
        lbl.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 11));
        lbl.setForeground(new Color(252, 211, 77));
        lbl.setBackground(DARK_SLATE);
        lbl.setOpaque(true);
        lbl.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        btnEdit  = new JButton("✏  Edit Data");
        btnHapus = new JButton("🗑  Hapus Data");

        for (JButton btn : new JButton[]{btnEdit, btnHapus}) {
            btn.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 11));
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setEnabled(false); // aktif saat baris dipilih
        }
        btnEdit.setBackground(new Color(59, 130, 246));  btnEdit.setForeground(Color.WHITE);
        btnHapus.setBackground(ROSE_RED);                btnHapus.setForeground(Color.WHITE);

        btnEdit.addActionListener(e  -> editInspeksiTerpilih());
        btnHapus.addActionListener(e -> hapusInspeksiTerpilih());

        // Aktifkan tombol saat baris dipilih
        tabelInspeksi.getSelectionModel().addListSelectionListener(ev -> {
            boolean ada = tabelInspeksi.getSelectedRow() >= 0;
            btnEdit.setEnabled(ada);
            btnHapus.setEnabled(ada);
        });

        JLabel info = new JLabel("← Pilih baris di tabel untuk mengaktifkan aksi");
        info.setFont(new Font("Plus Jakarta Sans", Font.ITALIC, 10));
        info.setForeground(new Color(148, 163, 184));

        p.add(lbl);
        p.add(btnEdit);
        p.add(btnHapus);
        p.add(info);
        return p;
    }

    private void hapusInspeksiTerpilih() {
        int row = tabelInspeksi.getSelectedRow();
        if (row < 0) return;
        String id = (String) modelTabel.getValueAt(row, 0);
        String produk = (String) modelTabel.getValueAt(row, 1);

        int konfirm = JOptionPane.showConfirmDialog(this,
            "<html>Hapus data inspeksi <b>" + id + "</b> (" + produk + ")?<br>"
            + "<font color='red'>Tindakan ini tidak bisa dibatalkan.</font></html>",
            "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (konfirm != JOptionPane.YES_OPTION) return;
        try {
            dataManager.hapusInspeksi(id);
            refreshTabel(dataManager.getDaftarInspeksi());
            refreshKPI();
            refreshGrafik();
            JOptionPane.showMessageDialog(this, "Data " + id + " berhasil dihapus.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
        } catch (DataTidakValidException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editInspeksiTerpilih() {
        int row = tabelInspeksi.getSelectedRow();
        if (row < 0) return;

        String id        = (String) modelTabel.getValueAt(row, 0);
        String tanggal   = (String) modelTabel.getValueAt(row, 3);
        String inspektur = (String) modelTabel.getValueAt(row, 4);
        double berat     = (Double) modelTabel.getValueAt(row, 5);
        double panjang   = (Double) modelTabel.getValueAt(row, 6);
        double lebar     = (Double) modelTabel.getValueAt(row, 7);
        int    cacat     = (Integer) modelTabel.getValueAt(row, 8);

        // Ambil catatan dari list (tidak tersimpan di tabel, ambil dari dataManager)
        String catatan = "";
        for (InspeksiDetail ins : dataManager.getDaftarInspeksi()) {
            if (ins.getIdInspeksi().equals(id)) { catatan = ins.getCatatanTambahan(); break; }
        }

        // Buat dialog edit
        JDialog dialog = new JDialog(this, "Edit Data Inspeksi — " + id, true);
        dialog.setSize(400, 460);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel bg = new JPanel(new GridBagLayout());
        bg.setBackground(new Color(241, 245, 249));
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(20, 25, 20, 25)));
        card.setPreferredSize(new Dimension(360, 400));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1.0; g.insets = new Insets(4, 0, 4, 0);

        JLabel title = new JLabel("Edit Inspeksi — " + id);
        title.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 14));
        title.setForeground(DARK_SLATE);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JTextField eTanggal   = buatTfEdit(tanggal);
        JTextField eInspektur = buatTfEdit(inspektur);
        JTextField eBerat     = buatTfEdit(String.valueOf(berat));
        JTextField ePanjang   = buatTfEdit(String.valueOf(panjang));
        JTextField eLebar     = buatTfEdit(String.valueOf(lebar));
        JTextField eCacat     = buatTfEdit(String.valueOf(cacat));
        JTextField eCatatan   = buatTfEdit(catatan);

        int y = 0;
        g.gridx = 0; g.gridy = y++; g.gridwidth = 2; card.add(title, g);
        // 2 kolom: tanggal | inspektur
        g.gridwidth = 1;
        g.gridy = y;   g.gridx = 0; card.add(buatFieldLabel("Tanggal"), g);
        g.gridx = 1;                card.add(buatFieldLabel("Inspektur"), g); y++;
        g.gridy = y;   g.gridx = 0; card.add(eTanggal, g);
        g.gridx = 1;                card.add(eInspektur, g); y++;
        // berat | cacat
        g.gridy = y;   g.gridx = 0; card.add(buatFieldLabel("Berat (g)"), g);
        g.gridx = 1;                card.add(buatFieldLabel("Jumlah Cacat"), g); y++;
        g.gridy = y;   g.gridx = 0; card.add(eBerat, g);
        g.gridx = 1;                card.add(eCacat, g); y++;
        // panjang | lebar
        g.gridy = y;   g.gridx = 0; card.add(buatFieldLabel("Panjang (cm)"), g);
        g.gridx = 1;                card.add(buatFieldLabel("Lebar (cm)"), g); y++;
        g.gridy = y;   g.gridx = 0; card.add(ePanjang, g);
        g.gridx = 1;                card.add(eLebar, g); y++;
        // catatan full width
        g.gridwidth = 2;
        g.gridy = y++; g.gridx = 0; card.add(buatFieldLabel("Catatan"), g);
        g.gridy = y++; card.add(eCatatan, g);

        // Tombol
        JPanel tombol = new JPanel(new GridLayout(1, 2, 10, 0));
        tombol.setOpaque(false);
        tombol.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        JButton btnSimpan = new JButton("💾  Simpan");
        JButton btnBatal  = new JButton("✕  Batal");
        btnSimpan.setBackground(EMERALD_GREEN); btnSimpan.setForeground(Color.WHITE);
        btnBatal.setBackground(new Color(241,245,249)); btnBatal.setForeground(new Color(71,85,105));
        for (JButton b : new JButton[]{btnSimpan, btnBatal}) {
            b.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 11));
            b.setFocusPainted(false); b.setBorderPainted(false);
            b.setPreferredSize(new Dimension(0, 36));
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        btnBatal.addActionListener(e -> dialog.dispose());
        btnSimpan.addActionListener(e -> {
            try {
                double nBerat   = Double.parseDouble(eBerat.getText().trim());
                double nPanjang = Double.parseDouble(ePanjang.getText().trim());
                double nLebar   = Double.parseDouble(eLebar.getText().trim());
                int    nCacat   = Integer.parseInt(eCacat.getText().trim());
                String nTanggal = eTanggal.getText().trim();
                String nInspek  = eInspektur.getText().trim();
                String nCatat   = eCatatan.getText().trim();

                dataManager.updateInspeksi(id, nBerat, nPanjang, nLebar, nCacat, nCatat, nTanggal, nInspek);
                refreshTabel(dataManager.getDaftarInspeksi());
                refreshKPI();
                refreshGrafik();
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Data " + id + " berhasil diperbarui!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Format angka tidak valid!", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (DataTidakValidException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        tombol.add(btnSimpan); tombol.add(btnBatal);
        g.gridy = y; card.add(tombol, g);

        bg.add(card);
        dialog.setContentPane(bg);
        dialog.setVisible(true);
    }

    // Helper textfield untuk dialog edit
    private JTextField buatTfEdit(String val) {
        JTextField tf = new JTextField(val);
        tf.setFont(new Font("Plus Jakarta Sans", Font.PLAIN, 11));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return tf;
    }

    private JPanel buatPanelHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(DARK_SLATE);
        p.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        // Kiri: ikon + judul
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel ico = new JLabel("⚙");
        ico.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        ico.setForeground(EMERALD_GREEN);
        JLabel lblJudul = new JLabel("AUTOMOTIVE MANUFACTURING QUALITY SYSTEM");
        lblJudul.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 15));
        lblJudul.setForeground(Color.WHITE);
        left.add(ico);
        left.add(lblJudul);

        // Kanan: nama user + badge role + tombol logout
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JLabel lblUser = new JLabel("👤 " + akunAktif.getNamaLengkap());
        lblUser.setFont(new Font("Plus Jakarta Sans", Font.PLAIN, 12));
        lblUser.setForeground(new Color(203, 213, 225));

        JLabel lblRole = new JLabel(akunAktif.getRole().name());
        lblRole.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 10));
        lblRole.setForeground(DARK_SLATE);
        lblRole.setBackground(akunAktif.getRole() == RolePengguna.PENGAWAS ? new Color(252, 211, 77) : EMERALD_GREEN);
        lblRole.setOpaque(true);
        lblRole.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

        JButton btnLogout = new JButton("Sign Out");
        btnLogout.setBackground(ROSE_RED);
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 11));
        btnLogout.setFocusPainted(false);
        btnLogout.setBorderPainted(false);
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> {
            dispose();
            new FrameLogin(new AkunManager()).setVisible(true);
        });

        right.add(lblUser);
        right.add(lblRole);
        right.add(btnLogout);

        p.add(left, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JPanel buatPanelFilter() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Color.WHITE);
        outer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        JLabel title = new JLabel("🔍 Filter & Sortir Data");
        title.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 12));
        title.setForeground(DARK_SLATE);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        outer.add(title, BorderLayout.NORTH);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setOpaque(false);

        tfTglMin  = buatTextField("2025-05-01", 9);
        tfTglMax  = buatTextField(LocalDate.now().toString(), 9);
        tfBeratMin = buatTextField("0", 6);
        tfBeratMax = buatTextField("9999", 6);
        cbSortTanggal = new JComboBox<>(new String[]{"⬇ Terbaru", "⬆ Terlama"});
        cbSortTanggal.setFont(new Font("Plus Jakarta Sans", Font.PLAIN, 11));

        JButton btnFilter = new JButton("Apply Filter");
        btnFilter.setBackground(DARK_SLATE);
        btnFilter.setForeground(Color.WHITE);
        btnFilter.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 11));
        btnFilter.setFocusPainted(false);
        btnFilter.setBorderPainted(false);
        btnFilter.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnFilter.addActionListener(e -> terapkanFilter());

        JButton btnReset = new JButton("Reset");
        btnReset.setBackground(new Color(241, 245, 249));
        btnReset.setForeground(new Color(71, 85, 105));
        btnReset.setFont(new Font("Plus Jakarta Sans", Font.PLAIN, 11));
        btnReset.setFocusPainted(false);
        btnReset.addActionListener(e -> {
            tfTglMin.setText("2025-05-01");
            tfTglMax.setText(LocalDate.now().toString());
            tfBeratMin.setText("0");
            tfBeratMax.setText("9999");
            refreshTabel(dataManager.getDaftarInspeksi());
        });

        row.add(buatLabelFilter("Dari Tanggal")); row.add(tfTglMin);
        row.add(buatLabelFilter("s/d")); row.add(tfTglMax);
        row.add(new JSeparator(JSeparator.VERTICAL)); // visual divider
        row.add(buatLabelFilter("Berat Min (g)")); row.add(tfBeratMin);
        row.add(buatLabelFilter("Max (g)")); row.add(tfBeratMax);
        row.add(new JSeparator(JSeparator.VERTICAL));
        row.add(buatLabelFilter("Urut")); row.add(cbSortTanggal);
        row.add(btnFilter);
        row.add(btnReset);

        outer.add(row, BorderLayout.CENTER);
        return outer;
    }

    // Helper: label kecil abu-abu untuk filter row
    private JLabel buatLabelFilter(String teks) {
        JLabel l = new JLabel(teks);
        l.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 10));
        l.setForeground(new Color(100, 116, 139));
        return l;
    }

    // Helper: textfield dengan border yang lebih rapi
    private JTextField buatTextField(String val, int cols) {
        JTextField tf = new JTextField(val, cols);
        tf.setFont(new Font("Plus Jakarta Sans", Font.PLAIN, 11));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        return tf;
    }

    private void terapkanFilter() {
        try {
            LocalDate tglMin = tfTglMin.getText().trim().isEmpty() ? null : LocalDate.parse(tfTglMin.getText().trim());
            LocalDate tglMax = tfTglMax.getText().trim().isEmpty() ? null : LocalDate.parse(tfTglMax.getText().trim());
            double bMin = Double.parseDouble(tfBeratMin.getText().trim());
            double bMax = Double.parseDouble(tfBeratMax.getText().trim());

            boolean asc = cbSortTanggal.getSelectedIndex() == 1; // index 1 = Terlama = ascending
            refreshTabel(dataManager.filterDanSortir(tglMin, tglMax, bMin, bMax, asc));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Format filter salah!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel buatPanelForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(5, 4, 5, 4);
        g.weightx = 1.0;

        cbProduk = new JComboBox<>();
        for (Produk p : dataManager.getDaftarProduk()) cbProduk.addItem(p);
        cbProduk.setFont(new Font("Plus Jakarta Sans", Font.PLAIN, 11));
        cbProduk.addActionListener(e -> tampilkanStandar());

        tfInspektur = new JTextField(akunAktif.getNamaLengkap());
        if (akunAktif.getRole() == RolePengguna.USER) tfInspektur.setEditable(false);
        tfTanggal  = new JTextField(LocalDate.now().toString());
        tfBerat    = new JTextField();
        tfPanjang  = new JTextField();
        tfLebar    = new JTextField();
        tfCacat    = new JTextField("0");
        tfCatatan  = new JTextField();

        // Style semua field
        for (JTextField tf : new JTextField[]{tfInspektur, tfTanggal, tfBerat, tfPanjang, tfLebar, tfCacat, tfCatatan}) {
            tf.setFont(new Font("Plus Jakarta Sans", Font.PLAIN, 11));
            tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        }

        // Baris 0: Item Produk — full width
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2; panel.add(buatFieldLabel("Item Produk"), g);
        g.gridy = 1; panel.add(cbProduk, g);
        g.gridwidth = 1;

        // Baris 2: Inspektur | Tanggal
        g.gridy = 2; g.gridx = 0; panel.add(buatFieldLabel("Petugas Inspektur"), g);
        g.gridx = 1; panel.add(buatFieldLabel("Tanggal Pemeriksaan"), g);
        g.gridy = 3; g.gridx = 0; panel.add(tfInspektur, g);
        g.gridx = 1; panel.add(tfTanggal, g);

        // Baris 4: Berat | Cacat
        g.gridy = 4; g.gridx = 0; panel.add(buatFieldLabel("Berat Aktual (gram)"), g);
        g.gridx = 1; panel.add(buatFieldLabel("Jumlah Cacat"), g);
        g.gridy = 5; g.gridx = 0; panel.add(tfBerat, g);
        g.gridx = 1; panel.add(tfCacat, g);

        // Baris 6: Panjang | Lebar
        g.gridy = 6; g.gridx = 0; panel.add(buatFieldLabel("Panjang Aktual (cm)"), g);
        g.gridx = 1; panel.add(buatFieldLabel("Lebar Aktual (cm)"), g);
        g.gridy = 7; g.gridx = 0; panel.add(tfPanjang, g);
        g.gridx = 1; panel.add(tfLebar, g);

        // Baris 8: Catatan — full width
        g.gridy = 8; g.gridx = 0; g.gridwidth = 2; panel.add(buatFieldLabel("Catatan Analisis"), g);
        g.gridy = 9; panel.add(tfCatatan, g);

        tampilkanStandar();
        return panel;
    }

    // Helper: label field kecil
    private JLabel buatFieldLabel(String teks) {
        JLabel l = new JLabel(teks);
        l.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 10));
        l.setForeground(new Color(71, 85, 105));
        return l;
    }

    private void tampilkanStandar() {
        Produk p = (Produk) cbProduk.getSelectedItem();
        if (p == null) return;
        String hint = String.format("Standar Baku Batas Presisi -> Berat: %.1fg | Pjg: %.1fcm | Lbr: %.1fcm", 
                p.getBeratStandar(), p.getPanjangStandar(), p.getLebarStandar());
        tfBerat.setToolTipText(hint);
    }

    private JPanel buatPanelTombol() {
        JPanel p = new JPanel(new GridLayout(1, 2, 10, 0));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        JButton btnTambah  = new JButton("💾  Save Record");
        JButton btnLaporan = new JButton("📄  View Report");

        for (JButton btn : new JButton[]{btnTambah, btnLaporan}) {
            btn.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 12));
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setPreferredSize(new Dimension(0, 38));
        }

        btnTambah.setBackground(EMERALD_GREEN);  btnTambah.setForeground(Color.WHITE);
        btnLaporan.setBackground(DARK_SLATE);    btnLaporan.setForeground(Color.WHITE);

        btnTambah.addActionListener(e -> tambahInspeksi());
        btnLaporan.addActionListener(e -> tampilkanLaporan());

        p.add(btnTambah);
        p.add(btnLaporan);
        return p;
    }

    private JPanel buatPanelKPI() {
        JPanel p = new JPanel(new GridLayout(1, 4, 12, 0));
        p.setBackground(BACKGROUND_BG);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        lblTotal  = buatKPICard("TOTAL INSPEKSI",  "0",  DARK_SLATE,          "📦");
        lblLolos  = buatKPICard("PRODUK LOLOS",    "0",  EMERALD_GREEN,       "✅");
        lblGagal  = buatKPICard("PRODUK REJECT",   "0",  ROSE_RED,            "❌");
        lblPersen = buatKPICard("PERSENTASE MUTU", "0%", new Color(79, 70, 229), "📈");

        p.add(lblTotal.getParent()); p.add(lblLolos.getParent());
        p.add(lblGagal.getParent()); p.add(lblPersen.getParent());
        return p;
    }

    private JLabel buatKPICard(String judul, String nilai, Color warna, String ikon) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, warna),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16))));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel lIco = new JLabel(ikon);
        lIco.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        JLabel lJ = new JLabel(judul);
        lJ.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 10));
        lJ.setForeground(new Color(100, 116, 139));
        top.add(lIco, BorderLayout.EAST);
        top.add(lJ,   BorderLayout.WEST);

        JLabel lN = new JLabel(nilai);
        lN.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 26));
        lN.setForeground(warna);

        card.add(top, BorderLayout.NORTH);
        card.add(lN,  BorderLayout.CENTER);
        return lN;
    }

    private JPanel buatPanelTabel() {
        String[] kolom = {"ID", "Produk", "Kategori", "Tanggal", "Inspektur", "Berat", "Panjang", "Lebar", "Cacat", "Status"};
        modelTabel = new DefaultTableModel(kolom, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tabelInspeksi = new JTable(modelTabel);
        tabelInspeksi.setRowHeight(32);
        tabelInspeksi.setFont(new Font("Plus Jakarta Sans", Font.PLAIN, 11));
        tabelInspeksi.setGridColor(new Color(241, 245, 249));
        tabelInspeksi.setShowHorizontalLines(true);
        tabelInspeksi.setShowVerticalLines(false);
        tabelInspeksi.setSelectionBackground(new Color(224, 242, 254));
        tabelInspeksi.setSelectionForeground(DARK_SLATE);
        tabelInspeksi.setIntercellSpacing(new Dimension(0, 1));

        // Header styling
        JTableHeader header = tabelInspeksi.getTableHeader();
        header.setFont(new Font("Plus Jakarta Sans", Font.BOLD, 11));
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(new Color(71, 85, 105));
        header.setPreferredSize(new Dimension(0, 36));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(226, 232, 240)));

        tabelInspeksi.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setFont(new Font("Plus Jakarta Sans", Font.PLAIN, 11));
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (!sel) {
                    String st = (String) t.getModel().getValueAt(r, 9);
                    setBackground(st.contains("LOLOS") ? new Color(240, 253, 244) : new Color(255, 241, 242));
                    setForeground(DARK_SLATE);
                }
                // Status column: bold
                if (c == 9) setFont(new Font("Plus Jakarta Sans", Font.BOLD, 11));
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(tabelInspeksi);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel pp = new JPanel(new BorderLayout());
        pp.setBackground(BACKGROUND_BG);
        pp.add(scroll, BorderLayout.CENTER);
        return pp;
    }

    private void tambahInspeksi() {
        try {
            Produk produk = (Produk) cbProduk.getSelectedItem();
            String inspektur = tfInspektur.getText().trim();
            String tanggal = tfTanggal.getText().trim();
            double berat   = Double.parseDouble(tfBerat.getText().trim());
            double panjang = Double.parseDouble(tfPanjang.getText().trim());
            double lebar   = Double.parseDouble(tfLebar.getText().trim());
            int    cacat   = Integer.parseInt(tfCacat.getText().trim());
            String catatan = tfCatatan.getText().trim();

            String idBaru = String.format("INS-%03d", dataManager.getTotalInspeksi() + 1);
            InspeksiDetail ins = new InspeksiDetail(idBaru, produk, tanggal, inspektur, berat, panjang, lebar, cacat, catatan);
            
            dataManager.tambahInspeksi(ins);
            refreshTabel(dataManager.getDaftarInspeksi());
            refreshKPI();
            refreshGrafik();

            JOptionPane.showMessageDialog(this, ins.isLolosQC() ? "Produk Sesuai Standar Presisi!" : "Produk Cacat/Deviasi Di Luar Batas Toleransi!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Kesalahan input data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshTabel(List<InspeksiDetail> list) {
        modelTabel.setRowCount(0);
        for (InspeksiDetail ins : list) {
            modelTabel.addRow(new Object[]{
                ins.getIdInspeksi(), ins.getProduk().getNamaProduk(), ins.getProduk().getKategori(),
                ins.getTanggalInspeksi(), ins.getNamaInspektur(), ins.getBeratAktual(),
                ins.getPanjangAktual(), ins.getLebarAktual(), ins.getJumlahCacat(),
                ins.isLolosQC() ? "✅ LOLOS" : "❌ REJECT"
            });
        }
    }

    private void refreshKPI() {
        lblTotal.setText(String.valueOf(dataManager.getTotalInspeksi()));
        lblLolos.setText(String.valueOf(dataManager.getTotalLolos()));
        lblGagal.setText(String.valueOf(dataManager.getTotalGagal()));
        lblPersen.setText(String.format("%.1f%%", dataManager.getPersentaseLolos()));
    }

    private void refreshGrafik() {
        panelPie.update(dataManager.getTotalLolos(), dataManager.getTotalGagal());
        panelBar.repaint();
    }

    private void tampilkanLaporan() {
        JTextArea area = new JTextArea(dataManager.generateLaporan());
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Laporan QC Ringkas", JOptionPane.INFORMATION_MESSAGE);
    }

    // ============================================================
    // [WEEK 1] Entry Point
    // ============================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new FrameLogin(new AkunManager()).setVisible(true);
        });
    }
}