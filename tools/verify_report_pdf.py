from recette_support import *
from pypdf import PdfReader
import io
b=login_as('director');year=time.strftime('%Y')
with b.opener.open(BASE+'RapportPdfServlet?annee='+year) as response:pdf=response.read()
(OUT/'rapport-annuel.pdf').write_bytes(pdf)
reader=PdfReader(io.BytesIO(pdf),strict=True)
check(len(reader.pages)>0,'PDF structure lisible par pypdf strict')
text=''.join(page.extract_text() for page in reader.pages)
first_column=[]
for page in reader.pages:
 page.extract_text(visitor_text=lambda value,cm,tm,font,size:first_column.append(value) if 43<=tm[4]<=45 else None)
normalized=re.sub(r'\s+','',''.join(first_column))
titles=db('SELECT titre FROM publication WHERE annee='+year).splitlines()
check(all(re.sub(r'\s+','',title) in normalized for title in titles),'PDF contient toutes les publications de l’année réelle')
check('Manifestations scientifiques' in text and 'Dotations' in text,'PDF inclut événements et données institutionnelles')
with b.opener.open(BASE+'RapportPdfServlet?annee=1900') as response:old=response.read()
old_text=''.join(p.extract_text() for p in PdfReader(io.BytesIO(old)).pages)
check(all(title not in old_text for title in titles),'PDF autre année ne reprend pas les publications courantes')
check(all('LIAS - Page '+str(i+1) in page.extract_text() for i,page in enumerate(reader.pages)),'pagination PDF cohérente')
save_results('pdf-content')
