import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class QueueSathi extends JFrame {
    private static final Path DATA = Paths.get("queuesathi-data.csv");
    private static final Color BG = new Color(15,17,22), PANEL = new Color(23,26,33), TEAL = new Color(92,218,194);
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"TOKEN","CUSTOMER","SERVICE","STATUS","CHECKED IN"},0) {
        public boolean isCellEditable(int r,int c){ return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel waiting = new JLabel("0"), serving = new JLabel("0"), completed = new JLabel("0");
    private int nextToken=1;

    public QueueSathi() {
        super("QueueSathi - Smart Queue Manager");
        setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(920,600); setMinimumSize(new Dimension(760,480)); setLocationRelativeTo(null);
        getContentPane().setBackground(BG); setLayout(new BorderLayout(16,16));
        JPanel top=new JPanel(new BorderLayout()); top.setOpaque(false); top.setBorder(BorderFactory.createEmptyBorder(22,24,4,24));
        JLabel title=new JLabel("<html><b style='font-size:22px'>QueueSathi</b><br><font color='#9298a5'>Smart queue manager for local businesses</font></html>");
        title.setForeground(Color.WHITE); top.add(title,BorderLayout.WEST);
        JButton add=button("+ Add visitor",TEAL,new Color(15,20,22)); add.addActionListener(e->addVisitor()); top.add(add,BorderLayout.EAST); add(top,BorderLayout.NORTH);

        JPanel center=new JPanel(new BorderLayout(0,14)); center.setOpaque(false); center.setBorder(BorderFactory.createEmptyBorder(0,24,20,24));
        JPanel cards=new JPanel(new GridLayout(1,3,12,0)); cards.setOpaque(false);
        cards.add(card("WAITING",waiting,new Color(245,190,85))); cards.add(card("NOW SERVING",serving,TEAL)); cards.add(card("COMPLETED",completed,new Color(160,173,192)));
        center.add(cards,BorderLayout.NORTH);

        JPanel queue=new JPanel(new BorderLayout()); queue.setBackground(PANEL); queue.setBorder(BorderFactory.createLineBorder(new Color(44,48,58)));
        JPanel bar=new JPanel(new BorderLayout(10,0)); bar.setOpaque(false); bar.setBorder(BorderFactory.createEmptyBorder(12,13,12,13));
        JLabel label=new JLabel("Visitor queue"); label.setForeground(Color.WHITE); label.setFont(new Font("SansSerif",Font.BOLD,14)); bar.add(label,BorderLayout.WEST);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,7,0)); actions.setOpaque(false);
        JTextField search=new JTextField(15); search.setToolTipText("Search queue"); search.setBackground(BG); search.setForeground(Color.WHITE);
        TableRowSorter<DefaultTableModel> sorter=new TableRowSorter<>(model); table.setRowSorter(sorter);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            void filter(){String q=search.getText().trim(); sorter.setRowFilter(q.isEmpty()?null:RowFilter.regexFilter("(?i)"+java.util.regex.Pattern.quote(q)));}
            public void insertUpdate(javax.swing.event.DocumentEvent e){filter();} public void removeUpdate(javax.swing.event.DocumentEvent e){filter();} public void changedUpdate(javax.swing.event.DocumentEvent e){filter();}
        });
        JButton call=button("Call next",new Color(42,48,58),Color.WHITE), done=button("Mark complete",new Color(42,48,58),Color.WHITE);
        call.addActionListener(e->callNext()); done.addActionListener(e->markComplete());
        actions.add(call); actions.add(done); actions.add(search); bar.add(actions,BorderLayout.EAST);
        table.setBackground(PANEL); table.setForeground(new Color(230,233,240)); table.setGridColor(new Color(44,48,58)); table.setSelectionBackground(new Color(43,56,63)); table.setRowHeight(39); table.setFont(new Font("SansSerif",Font.PLAIN,12)); table.setFillsViewportHeight(true);
        table.getTableHeader().setBackground(new Color(29,32,40)); table.getTableHeader().setForeground(new Color(165,172,185)); table.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,10)); table.getTableHeader().setPreferredSize(new Dimension(0,34));
        JScrollPane scroll=new JScrollPane(table); scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.getViewport().setBackground(PANEL);
        queue.add(bar,BorderLayout.NORTH); queue.add(scroll,BorderLayout.CENTER); center.add(queue,BorderLayout.CENTER);
        JLabel note=new JLabel("Offline mode  |  Data saves beside the application in queuesathi-data.csv"); note.setForeground(new Color(115,122,136)); note.setFont(new Font("SansSerif",Font.PLAIN,10)); center.add(note,BorderLayout.SOUTH);
        add(center,BorderLayout.CENTER); load(); refreshStats();
    }

    private JPanel card(String name,JLabel value,Color color){
        JPanel p=new JPanel(new BorderLayout(0,8)); p.setBackground(PANEL); p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(44,48,58)),BorderFactory.createEmptyBorder(13,15,13,15)));
        JLabel n=new JLabel(name); n.setForeground(new Color(145,151,165)); n.setFont(new Font("SansSerif",Font.BOLD,10)); value.setForeground(color); value.setFont(new Font("SansSerif",Font.BOLD,23)); p.add(n,BorderLayout.NORTH);p.add(value,BorderLayout.CENTER);return p;
    }
    private JButton button(String text,Color bg,Color fg){JButton b=new JButton(text);b.setBackground(bg);b.setForeground(fg);b.setFocusPainted(false);b.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));b.setFont(new Font("SansSerif",Font.BOLD,11));return b;}
    private void addVisitor(){
        JTextField name=new JTextField(), service=new JTextField();
        JPanel form=new JPanel(new GridLayout(0,1,4,5));form.add(new JLabel("Customer name"));form.add(name);form.add(new JLabel("Service / reason"));form.add(service);
        if(JOptionPane.showConfirmDialog(this,form,"Add visitor",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        if(name.getText().trim().isEmpty()||service.getText().trim().isEmpty()){JOptionPane.showMessageDialog(this,"Enter a name and service.");return;}
        model.addRow(new Object[]{String.format("Q-%03d",nextToken++),name.getText().trim(),service.getText().trim(),"WAITING",LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))});save();refreshStats();
    }
    private void callNext(){for(int i=0;i<model.getRowCount();i++)if("WAITING".equals(model.getValueAt(i,3))){model.setValueAt("SERVING",i,3);table.setRowSelectionInterval(i,i);save();refreshStats();return;}JOptionPane.showMessageDialog(this,"Nobody is waiting right now.");}
    private void markComplete(){int v=table.getSelectedRow();if(v<0){JOptionPane.showMessageDialog(this,"Select a visitor first.");return;}model.setValueAt("COMPLETED",table.convertRowIndexToModel(v),3);save();refreshStats();}
    private void refreshStats(){int w=0,s=0,c=0;for(int i=0;i<model.getRowCount();i++){String x=(String)model.getValueAt(i,3);if(x.equals("WAITING"))w++;else if(x.equals("SERVING"))s++;else c++;}waiting.setText(""+w);serving.setText(""+s);completed.setText(""+c);}
    private String quote(String s){return "\""+s.replace("\"","\"\"")+"\"";}
    private void save(){try(BufferedWriter out=Files.newBufferedWriter(DATA,StandardCharsets.UTF_8)){for(int r=0;r<model.getRowCount();r++){for(int c=0;c<5;c++){if(c>0)out.write(',');out.write(quote(String.valueOf(model.getValueAt(r,c))));}out.newLine();}}catch(IOException e){JOptionPane.showMessageDialog(this,"Could not save data: "+e.getMessage());}}
    private void load(){if(!Files.exists(DATA))return;try{for(String line:Files.readAllLines(DATA,StandardCharsets.UTF_8)){List<String> a=parse(line);if(a.size()==5){model.addRow(a.toArray());try{nextToken=Math.max(nextToken,Integer.parseInt(a.get(0).substring(2))+1);}catch(Exception ignored){}}}}catch(IOException e){JOptionPane.showMessageDialog(this,"Could not load data: "+e.getMessage());}}
    private List<String> parse(String line){List<String> out=new ArrayList<>();StringBuilder b=new StringBuilder();boolean q=false;for(int i=0;i<line.length();i++){char c=line.charAt(i);if(c=='"'){if(q&&i+1<line.length()&&line.charAt(i+1)=='"'){b.append('"');i++;}else q=!q;}else if(c==','&&!q){out.add(b.toString());b.setLength(0);}else b.append(c);}out.add(b.toString());return out;}
    public static void main(String[] args){SwingUtilities.invokeLater(()->new QueueSathi().setVisible(true));}
}
