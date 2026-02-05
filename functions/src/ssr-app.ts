import * as functions from 'firebase-functions';
import {getFirestore} from './utils/firestore';
/**
 * SSR leve para post/produto/perfil.
 * Busca dados no Firestore e retorna HTML com metatags OG/Twitter e deep link.
 * Rotas esperadas: /post/{id}, /product/{id}, /user/{id}
 */
const db = getFirestore();

type PageKind = 'post' | 'product' | 'user';

const buildHtml = (opts: {
  title: string;
  description?: string;
  image?: string;
  url: string;
  deepLink: string;
  type: PageKind;
}) => {
  const {title, description, image, url, deepLink, type} = opts;
  const safeTitle = title || 'TaskGo';
  const safeDesc = description || 'Veja mais no TaskGo';
  const safeImage = image || 'https://taskgoapps.com/static/share-default.png';

  return `<!doctype html>
<html lang="pt-BR">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>${safeTitle}</title>
  <meta name="description" content="${safeDesc}">
  <link rel="canonical" href="${url}">
  <link rel="alternate" href="${deepLink}">

  <!-- Open Graph -->
  <meta property="og:title" content="${safeTitle}">
  <meta property="og:description" content="${safeDesc}">
  <meta property="og:type" content="article">
  <meta property="og:url" content="${url}">
  <meta property="og:image" content="${safeImage}">

  <!-- Twitter -->
  <meta name="twitter:card" content="summary_large_image">
  <meta name="twitter:title" content="${safeTitle}">
  <meta name="twitter:description" content="${safeDesc}">
  <meta name="twitter:image" content="${safeImage}">

  <!-- App Links / Android -->
  <meta property="al:android:url" content="${deepLink}">
  <meta property="al:android:package" content="com.taskgoapp.taskgo">
  <meta property="al:android:app_name" content="TaskGo">

  <!-- Fallback redirect para o deep link -->
  <script>
    setTimeout(function() { window.location.href = '${deepLink}'; }, 300);
  </script>
</head>
<body style="font-family: Arial, sans-serif; padding: 24px; line-height: 1.5;">
  <h1>${safeTitle}</h1>
  <p>${safeDesc}</p>
  <p><a href="${deepLink}">Abrir no app</a></p>
  <p><a href="https://taskgoapps.com">TaskGo</a> • ${type}</p>
</body>
</html>`;
};

export const ssrAppPage = functions.https.onRequest(async (req, res) => {
  try {
    const path = req.path.replace(/^\/+/, '');
    const segments = path.split('/').filter(Boolean);
    const kind = segments[0] as PageKind;
    const id = segments[1];

    if (!id || !['post', 'product', 'user'].includes(kind)) {
      res.status(404).send('Not found');
      return;
    }

    // Seleciona coleção e campos relevantes
    let title = 'TaskGo';
    let desc = 'Veja mais no TaskGo';
    let image: string | undefined;

    if (kind === 'post') {
      // CRÍTICO: Posts estão em locations/{locationId}/posts
      // Como não temos locationId inicialmente, precisamos buscar em todas as localizações
      // Mas agora os documentos têm locationId armazenado, então podemos usar diretamente quando encontrado
      const locationsSnapshot = await db.collection('locations').limit(100).get();
      let found = false;
      
      // Buscar post em todas as localizações
      for (const locationDoc of locationsSnapshot.docs) {
        const postDoc = await locationDoc.ref.collection('posts').doc(id).get();
        if (postDoc.exists) {
          const data = postDoc.data() || {};
          // Usar locationId do documento se disponível, senão usar o locationId da coleção
          // const postLocationId = data.locationId as string || locationDoc.id; // Não usado no momento
          title = data.text || 'Post no TaskGo';
          desc = data.userName ? `Post de ${data.userName}` : desc;
          const media = Array.isArray(data.mediaUrls) ? data.mediaUrls : [];
          image = media[0];
          found = true;
          break;
        }
      }
      
      if (!found) {
        res.status(404).send('Post não encontrado');
        return;
      }
    } else if (kind === 'product') {
      // CRÍTICO: Products estão em locations/{locationId}/products
      // Como não temos locationId inicialmente, precisamos buscar em todas as localizações
      // Mas agora os documentos têm locationId armazenado, então podemos usar diretamente quando encontrado
      const locationsSnapshot = await db.collection('locations').limit(100).get();
      let found = false;
      
      // Buscar produto em todas as localizações
      for (const locationDoc of locationsSnapshot.docs) {
        const productDoc = await locationDoc.ref.collection('products').doc(id).get();
        if (productDoc.exists) {
          const data = productDoc.data() || {};
          // Usar locationId do documento se disponível, senão usar o locationId da coleção
          // const productLocationId = data.locationId as string || locationDoc.id; // Não usado no momento
          title = data.title || 'Produto no TaskGo';
          desc = data.description || desc;
          const imgs = Array.isArray(data.imageUrls) ? data.imageUrls : [];
          image = imgs[0];
          found = true;
          break;
        }
      }
      
      if (!found) {
        res.status(404).send('Produto não encontrado');
        return;
      }
    } else if (kind === 'user') {
      const doc = await db.collection('users').doc(id).get();
      if (!doc.exists) {
        res.status(404).send('Perfil não encontrado');
        return;
      }
      const data = doc.data() || {};
      title = data.displayName || 'Perfil TaskGo';
      desc = data.bio || desc;
      image = data.photoURL;
    }

    const url = `https://taskgoapps.com/${kind}/${id}`;
    const deepLink = `https://taskgoapps.com/${kind}/${id}`;

    res.set('Cache-Control', 'public, max-age=60, s-maxage=300');
    res.status(200).send(buildHtml({
      title,
      description: desc,
      image,
      url,
      deepLink,
      type: kind,
    }));
  } catch (e) {
    console.error('ssrAppPage error', e);
    res.status(500).send('Internal error');
  }
});

